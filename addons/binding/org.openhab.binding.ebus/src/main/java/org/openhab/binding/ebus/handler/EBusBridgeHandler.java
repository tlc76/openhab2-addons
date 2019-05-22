/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.handler;

import static org.openhab.binding.ebus.EBusBindingConstants.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.internal.EBusAdvancedLogging;
import org.openhab.binding.ebus.internal.EBusHandlerFactory;
import org.openhab.binding.ebus.internal.EBusLibClient;
import org.openhab.binding.ebus.thing.EBusTypeProvider;
import org.openhab.binding.ebus.thing.IEBusTypeProviderListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.core.IEBusConnectorEventListener;
import de.csdev.ebus.core.IEBusController;
import de.csdev.ebus.service.metrics.EBusMetricsService;
import de.csdev.ebus.service.parser.IEBusParserListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 * The {@link EBusBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBridgeHandler extends BaseBridgeHandler
        implements IEBusParserListener, IEBusConnectorEventListener, IEBusTypeProviderListener {

    private final Logger logger = LoggerFactory.getLogger(EBusBridgeHandler.class);
    private final Logger loggerExt = LoggerFactory.getLogger("org.openhab.ebus-ext");

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(EBusBindingConstants.THING_TYPE_EBUS_BRIDGE);

    private EBusLibClient libClient;

    private EBusHandlerFactory handlerFactory;

    private EBusTypeProvider typeProvider;

    private EBusAdvancedLogging advanceLogger;

    private ScheduledFuture<?> metricsRefreshSchedule;

    public EBusBridgeHandler(@NonNull Bridge bridge, EBusTypeProvider typeProvider, EBusHandlerFactory handlerFactory) {

        super(bridge);

        // reference configuration
        this.typeProvider = typeProvider;
        this.handlerFactory = handlerFactory;
    }

    /**
     * Returns the eBUS core lib client
     *
     * @return
     */
    public EBusLibClient getLibClient() {
        return libClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {

        // initialize the ebus client wrapper
        libClient = new EBusLibClient(typeProvider.getCommandRegistry());

        // add the discovery service
        handlerFactory.disposeDiscoveryService(this);
        handlerFactory.registerDiscoveryService(this);

        // libClient = new EBusLibClient();
        Configuration configuration = getThing().getConfiguration();

        String ipAddress = null;
        BigDecimal port = null;
        String networkDriver = DRIVER_RAW;

        String serialPort = null;
        String serialPortDriver = DRIVER_NRJAVASERIAL;

        String masterAddressStr = null;
        Byte masterAddress = (byte) 0xFF;

        try {
            ipAddress = (String) configuration.get(IP_ADDRESS);
            port = (BigDecimal) configuration.get(PORT);
            networkDriver = (String) configuration.get(NETWORK_DRIVER);

            masterAddressStr = (String) configuration.get(MASTER_ADDRESS);
            serialPort = (String) configuration.get(SERIAL_PORT);
            serialPortDriver = (String) configuration.get(SERIAL_PORT_DRIVER);

            if (configuration.get(ADVANCED_LOGGING).equals(Boolean.TRUE)) {
                logger.warn("Enable advanced logging for eBUS commands!");
                advanceLogger = new EBusAdvancedLogging();
            }

        } catch (Exception e) {
            logger.warn("Cannot set parameters!", e);
        }

        if (StringUtils.isNotEmpty(masterAddressStr)) {
            masterAddress = EBusUtils.toByte(masterAddressStr);
        }

        if (StringUtils.isNotEmpty(ipAddress) && port != null) {

            // use ebusd as high level driver
            if (networkDriver.equals(DRIVER_EBUSD)) {
                libClient.setEbusdConnection(ipAddress, port.intValue());
            } else {
                libClient.setTCPConnection(ipAddress, port.intValue());
            }
        }

        if (StringUtils.isNotEmpty(serialPort)) {
            libClient.setSerialConnection(serialPort, serialPortDriver);
        }

        if (masterAddress != null && !EBusUtils.isMasterAddress(masterAddress)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "eBUS master address is not a valid master address!");

            return;
        }

        if (!libClient.isConnectionValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Network address and Port either Serial Port must be set!");

            return;
        }

        libClient.initClient(masterAddress);

        // add before other listeners, better to read in logs
        if (advanceLogger != null) {
            libClient.getClient().addEBusParserListener(advanceLogger);
        }

        // add listeners
        libClient.getClient().addEBusEventListener(this);
        libClient.getClient().addEBusParserListener(this);

        typeProvider.addTypeProviderListener(this);

        startMetricScheduler();

        // start eBus controller
        libClient.startClient();

        updateStatus(ThingStatus.ONLINE);
    }

    private void startMetricScheduler() {

        if (metricsRefreshSchedule != null) {
            metricsRefreshSchedule.cancel(true);
            metricsRefreshSchedule = null;
        }

        metricsRefreshSchedule = scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    EBusMetricsService metricsService = libClient.getClient().getMetricsService();
                    IEBusController controller = libClient.getClient().getController();

                    EBusBridgeHandler that = EBusBridgeHandler.this;
                    ThingUID thingUID = that.getThing().getUID();

                    that.updateState(new ChannelUID(thingUID, METRICS, RECEIVED_TELEGRAMS),
                            new DecimalType(metricsService.getReceived()));
                    that.updateState(new ChannelUID(thingUID, METRICS, FAILED_TELEGRAMS),
                            new DecimalType(metricsService.getFailed()));
                    that.updateState(new ChannelUID(thingUID, METRICS, RESOLVED_TELEGRAMS),
                            new DecimalType(metricsService.getResolved()));
                    that.updateState(new ChannelUID(thingUID, METRICS, UNRESOLVED_TELEGRAMS),
                            new DecimalType(metricsService.getUnresolved()));
                    that.updateState(new ChannelUID(thingUID, METRICS, FAILED_RATIO),
                            new DecimalType(metricsService.getFailureRatio()));
                    that.updateState(new ChannelUID(thingUID, METRICS, UNRESOLVED_RATIO),
                            new DecimalType(metricsService.getUnresolvedRatio()));

                    that.updateState(new ChannelUID(thingUID, METRICS, SEND_RECEIVE_ROUNDTRIP_TIME),
                            new DecimalType((int) controller.getLastSendReceiveRoundtripTime() / 1000));

                } catch (Exception e) {
                    logger.error("error!", e);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {

        if (metricsRefreshSchedule != null) {
            metricsRefreshSchedule.cancel(true);
            metricsRefreshSchedule = null;
        }

        typeProvider.removeTypeProviderListener(this);

        if (advanceLogger != null) {
            libClient.getClient().removeEBusParserListener(advanceLogger);
            advanceLogger.close();
            advanceLogger = null;
        }

        // remove discovery service
        handlerFactory.disposeDiscoveryService(this);

        if (libClient != null) {

            libClient.stopClient();
            libClient.getClient().dispose();

            libClient = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.csdev.ebus.service.parser.IEBusParserListener#onTelegramResolved(de.csdev.ebus.command.IEBusCommandMethod,
     * java.util.Map, byte[], java.lang.Integer)
     */
    @Override
    public void onTelegramResolved(IEBusCommandMethod commandChannel, Map<String, Object> result, byte[] receivedData,
            Integer sendQueueId) {

        boolean noHandler = true;

        String source = EBusUtils.toHexDumpString(receivedData[0]);
        String destination = EBusUtils.toHexDumpString(receivedData[1]);

        logger.debug("Received telegram from address {} to {} with command {}", source, destination,
                commandChannel.getParent().getId());

        if (!this.isInitialized()) {
            logger.warn("eBUS bridge is not initialized! Unable to process resolved telegram!");
            return;
        }

        // loop over all child nodes
        for (Thing thing : getThing().getThings()) {

            EBusHandler handler = (EBusHandler) thing.getHandler();

            if (handler != null) {

                // check if this handler can process this telegram
                if (handler.supportsTelegram(receivedData, commandChannel)) {

                    // process
                    handler.handleReceivedTelegram(commandChannel, result, receivedData, sendQueueId);
                    noHandler = false;
                }
            }
        }

        if (noHandler) {
            logger.debug("No handler has accepted the command {} from {} to {} ...", commandChannel.getParent().getId(),
                    source, destination);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.IEBusConnectorEventListener#onTelegramException(de.csdev.ebus.core.EBusDataException,
     * java.lang.Integer)
     */
    @Override
    public void onTelegramException(EBusDataException exception, Integer sendQueueId) {
        logger.debug("eBUS telegram error; {}", exception.getLocalizedMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.IEBusConnectorEventListener#onConnectionException(java.lang.Exception)
     */
    @Override
    public void onConnectionException(Exception e) {

        if (metricsRefreshSchedule != null) {
            metricsRefreshSchedule.cancel(true);
            metricsRefreshSchedule = null;
        }

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandler#handleCommand(org.eclipse.smarthome.core.thing.ChannelUID,
     * org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, Command command) {
        // noop for bridge
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.IEBusConnectorEventListener#onTelegramReceived(byte[], java.lang.Integer)
     */
    @Override
    public void onTelegramReceived(byte[] receivedData, Integer sendQueueId) {
        Bridge bridge = getThing();

        if (bridge.getStatus() != ThingStatus.ONLINE) {

            // bring the bridge back online
            updateStatus(ThingStatus.ONLINE);

            // start the metrics scheduler
            startMetricScheduler();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.ebus.thing.IEBusTypeProviderListener#onTypeProviderUpdate()
     */
    @Override
    public void onTypeProviderUpdate() {

        // update all handlers
        for (Thing thing : getThing().getThings()) {
            EBusHandler handler = (EBusHandler) thing.getHandler();
            if (handler != null) {
                handler.updateHandler();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.service.parser.IEBusParserListener#onTelegramResolveFailed(de.csdev.ebus.command.
     * IEBusCommandMethod, byte[], java.lang.Integer, java.lang.String)
     */
    @Override
    public void onTelegramResolveFailed(IEBusCommandMethod commandChannel, byte[] receivedData, Integer sendQueueId,
            String exceptionMessage) {
        if (loggerExt.isDebugEnabled()) {
            loggerExt.debug("Unknown telegram {}", EBusUtils.toHexDumpString(receivedData));
        }
    }
}
