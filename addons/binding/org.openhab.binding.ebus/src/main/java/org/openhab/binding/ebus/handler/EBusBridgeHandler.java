/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.internal.EBusHandlerFactory;
import org.openhab.binding.ebus.internal.EBusLibClient;
import org.openhab.binding.ebus.thing.EBusTypeProvider;
import org.openhab.binding.ebus.thing.IEBusTypeProviderListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.core.IEBusConnectorEventListener;
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

    public EBusBridgeHandler(@NonNull Bridge bridge, EBusTypeProvider typeProvider, EBusHandlerFactory handlerFactory) {

        super(bridge);

        // reference configuration
        this.typeProvider = typeProvider;
        this.handlerFactory = handlerFactory;
    }

    /**
     * @return
     */
    public EBusLibClient getLibClient() {
        return libClient;
    }

    @SuppressWarnings("null")
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
        String serialPort = null;

        String masterAddressStr = null;
        Byte masterAddress = (byte) 0xFF;

        try {
            ipAddress = (String) configuration.get(IP_ADDRESS);
            port = (BigDecimal) configuration.get(PORT);
            masterAddressStr = (String) configuration.get(MASTER_ADDRESS);
            serialPort = (String) configuration.get(SERIAL_PORT);

        } catch (Exception e) {
            logger.debug("Cannot set parameters!", e);
        }

        if (StringUtils.isNotEmpty(masterAddressStr)) {
            masterAddress = EBusUtils.toByte(masterAddressStr);
        }

        if (StringUtils.isNotEmpty(ipAddress) && port != null) {
            libClient.setTCPConnection(ipAddress, port.intValue());
        }

        if (StringUtils.isNotEmpty(serialPort)) {
            // create a dummy connection
            if (serialPort.equals("dummy")) {
                libClient.setDummyConnection(scheduler);
            } else {
                libClient.setSerialConnection(serialPort);
            }

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

        // add listeners
        libClient.getClient().addEBusEventListener(this);
        libClient.getClient().addEBusParserListener(this);

        // start eBus controller
        libClient.startClient();

        typeProvider.addTypeProviderListener(this);

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {

        typeProvider.removeTypeProviderListener(this);

        // remove discovery service
        handlerFactory.disposeDiscoveryService(this);

        if (libClient != null) {

            libClient.stopClient();
            libClient.getClient().dispose();

            libClient = null;
        }
    }

    @Override
    public void onTelegramResolved(IEBusCommandMethod commandChannel, Map<String, Object> result, byte[] receivedData,
            Integer sendQueueId) {

        boolean noHandler = true;

        String source = EBusUtils.toHexDumpString(receivedData[0]);
        String destination = EBusUtils.toHexDumpString(receivedData[1]);

        logger.info("Received telegram from address {} to {} with command {}", source, destination,
                commandChannel.getParent().getId());

        if (!this.isInitialized()) {
            logger.warn("eBUS bridge is not initialized! Unable to process resolved telegram!");
            return;
        }

        if (getThing().getThings() != null) {

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
        }

        if (noHandler) {
            logger.info("No handler has accepted the command {} from {} to {} ...", commandChannel.getParent().getId(),
                    source, destination);
        }
    }

    @Override
    public void onTelegramException(EBusDataException exception, Integer sendQueueId) {
        if (logger.isDebugEnabled()) {
            logger.debug("eBUS telegram error; {}", exception.getLocalizedMessage());
        }
    }

    @Override
    public void onConnectionException(Exception e) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, Command command) {
        // noop for bridge
    }

    @Override
    public void onTelegramReceived(byte[] receivedData, Integer sendQueueId) {
        // noop
    }

    @Override
    public void onTelegramResolveFailed(byte[] receivedData, Integer sendQueueId) {
        if (loggerExt.isDebugEnabled()) {
            loggerExt.debug("Unknown telegram {}", EBusUtils.toHexDumpString(receivedData));
        }
    }

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
}
