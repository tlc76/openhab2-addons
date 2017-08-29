/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.handler;

import static org.openhab.binding.ebus.EBusBindingConstants.CHANNEL_1;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.internal.EBusLibClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClientConfiguration;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.core.EBusConnectorEventListener;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.service.parser.EBusParserListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 * The {@link EBusBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBridgeHandler extends BaseBridgeHandler implements EBusParserListener, EBusConnectorEventListener {

    private final Logger logger = LoggerFactory.getLogger(EBusBridgeHandler.class);

    private EBusLibClient libClient;

    public EBusBridgeHandler(@NonNull Bridge bridge, EBusClientConfiguration clientConfiguration) {
        super(bridge);

        // initialize the ebus client wrapper
        libClient = new EBusLibClient(clientConfiguration);
    }

    public EBusLibClient getLibClient() {
        return libClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_1)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {

        // libClient = new EBusLibClient();
        Configuration configuration = getThing().getConfiguration();

        // IEBusConnection connection = null;
        String ipAddress = null;
        BigDecimal port = null;

        String serialPort = null;

        String masterAddressStr = null;
        Byte masterAddress = (byte) 0x00;

        try {
            ipAddress = (String) configuration.get("ipAddress");
            port = (BigDecimal) configuration.get("port");
            masterAddressStr = (String) configuration.get("masterAddress");
            serialPort = (String) configuration.get("serialPort");
        } catch (Exception e) {
            logger.debug("Cannot set network parameters.", e);
        }

        if (StringUtils.isNotEmpty(masterAddressStr)) {
            masterAddress = EBusUtils.toByte(masterAddressStr);
        }

        if (StringUtils.isNotEmpty(ipAddress) && port != null) {
            libClient.setTCPConnection(ipAddress, port.intValue());
        }

        if (StringUtils.isNotEmpty(serialPort)) {
            libClient.setSerialConnection(serialPort);
        }

        if (!EBusUtils.isMasterAddress(masterAddress)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "eBus master address is not a valid master address!");

            return;
        }

        if (!libClient.isConnectionValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Network address and Port either Serial Port must be set!");

            return;
        }

        libClient.initClient(masterAddress);

        // add listeners
        libClient.getClient().getController().addEBusEventListener(this);
        libClient.getClient().getResolverService().addEBusParserListener(this);

        scheduler.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    // libClient.getClient().getDeviceTableService().startDeviceScan();
                    libClient.getClient().getDeviceTableService().sendIdentificationRequest((byte) 0x76);
                } catch (Exception e) {
                    logger.error("error!", e);
                }
            }
        }, 30, TimeUnit.SECONDS);

        // start eBus controller
        libClient.startClient();
    }

    @Override
    public void dispose() {
        if (libClient != null) {

            // remove listeners
            libClient.getClient().getController().removeEBusEventListener(this);
            libClient.getClient().getResolverService().removeEBusParserListener(this);

            libClient.stopClient();
            libClient = null;
        }
    }

    @Override
    public void onTelegramResolved(IEBusCommandMethod commandChannel, Map<String, Object> result, byte[] receivedData,
            Integer sendQueueId) {

        String sourceAddress = EBusUtils.toHexDumpString(receivedData[0]);
        String targetAddress = EBusUtils.toHexDumpString(receivedData[1]);
        // Byte destinationAddress = command.getDestinationAddress();

        logger.info("Received telegram from master address {} with command {}", sourceAddress,
                commandChannel.getParent().getId());

        if (getThing().getThings() != null) {

            for (Thing thing : getThing().getThings()) {

                String masterAddress = (String) thing.getConfiguration()
                        .get(EBusBindingConstants.CONFIG_MASTER_ADDRESS);

                String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.CONFIG_SLAVE_ADDRESS);

                if (sourceAddress.equals(masterAddress) || targetAddress.equals(slaveAddress)) {

                    // logger.info("Found thing with master address {} or slave address {} ...", masterAddress,
                    // slaveAddress);

                    for (Entry<String, Object> resultEntry : result.entrySet()) {

                        ChannelUID m = new ChannelUID(thing.getUID(),
                                commandChannel.getParent().getId().replace('.', '-'), resultEntry.getKey());

                        Channel channel = thing.getChannel(m.getId());

                        if (channel != null) {
                            // logger.info("Found channel @ thing ...");

                            if (channel.getAcceptedItemType().equals("Number")) {
                                if (resultEntry.getValue() != null) {
                                    if (resultEntry.getValue() instanceof BigDecimal) {
                                        updateState(channel.getUID(),
                                                new DecimalType((BigDecimal) resultEntry.getValue()));
                                    } else {
                                        logger.warn("Unexpected datatype {} for channel {} !",
                                                resultEntry.getValue().getClass().getSimpleName(), m.getAsString());
                                    }

                                }

                            } else if (channel.getAcceptedItemType().equals("String")) {
                                if (resultEntry.getValue() instanceof String) {
                                    updateState(channel.getUID(), new StringType((String) resultEntry.getValue()));
                                } else if (resultEntry.getValue() instanceof byte[]) {
                                    // show bytes as hex string
                                    updateState(channel.getUID(), new StringType(
                                            EBusUtils.toHexDumpString((byte[]) resultEntry.getValue()).toString()));
                                }

                            } else if (channel.getAcceptedItemType().equals("Switch")) {
                                if (resultEntry.getValue() instanceof Boolean) {
                                    boolean isOn = ((Boolean) resultEntry.getValue()).booleanValue();
                                    updateState(channel.getUID(), isOn ? OnOffType.ON : OnOffType.OFF);
                                }

                            } else if (channel.getAcceptedItemType().equals("DateTime")) {
                                if (resultEntry.getValue() instanceof Calendar) {
                                    this.updateState(channel.getUID(),
                                            new DateTimeType((Calendar) resultEntry.getValue()));
                                }

                            }

                        }

                    }

                }
            }

        }
    }

    @Override
    public void onTelegramReceived(byte[] receivedData, Integer sendQueueId) {
        if (EBusBridgeHandler.this.getThing().getStatus().equals(ThingStatus.INITIALIZING)) {
            // set status to online if we are able to receive valid telegrams
            updateStatus(ThingStatus.ONLINE);
        }

        // logger.error(EBusUtils.toHexDumpString(receivedData).toString());

        // 6,7
        // if (receivedData[2] == (byte) 0x50 && receivedData[3] == (byte) 0x22 && receivedData[6] == (byte) 0xFB
        // && receivedData[7] == (byte) 0x02) {
        // if (logger.isErrorEnabled()) {
        // logger.error(EBusUtils.toHexDumpString(receivedData).toString());
        // }
        // }
        //
        // if (receivedData[2] == (byte) 0x07 && receivedData[3] == (byte) 0x00) {
        // if (logger.isErrorEnabled()) {
        // logger.error(EBusUtils.toHexDumpString(receivedData).toString());
        // }
        // }

    }

    @Override
    public void onTelegramException(EBusDataException exception, Integer sendQueueId) {
        // if (logger.isErrorEnabled()) {
        // logger.error("eBus telegram error; {}", exception.getLocalizedMessage());
        // }
    }

    @Override
    public void onConnectionException(Exception e) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    }
}
