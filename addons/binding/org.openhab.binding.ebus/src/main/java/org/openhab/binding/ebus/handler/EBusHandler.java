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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.ebus.internal.EBusBindingUtils;
import org.openhab.binding.ebus.internal.EBusLibClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusConsts;
import de.csdev.ebus.utils.EBusDateTime;
import de.csdev.ebus.utils.EBusUtils;

/**
 * The {@link EBusHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(EBusHandler.class);

    private Map<String, ScheduledFuture<?>> pollings = new HashMap<>();

    private Random random = new Random(12);

    /**
     * @param thing
     */
    public EBusHandler(@NonNull Thing thing) {
        super(thing);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandler#handleCommand(org.eclipse.smarthome.core.thing.ChannelUID,
     * org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (!(command instanceof RefreshType)) {
            Channel channel = thing.getChannel(channelUID.getId());

            if (channel != null) {
                try {
                    ByteBuffer telegram = getLibClient().generateSetterTelegram(thing, channel, command);
                    if (telegram != null) {
                        getLibClient().sendTelegram(telegram);
                    }
                } catch (EBusTypeException e) {
                    logger.error("error!", e);
                }

            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {
        cancelPollingJobs();
    }

    /**
     *
     */
    private void cancelPollingJobs() {
        for (ScheduledFuture<?> pollingJob : pollings.values()) {
            pollingJob.cancel(true);
        }
        pollings.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {

        if (getBridge() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge defined!");

        } else if (getBridge().getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            initializePolling();

        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }

    }

    /**
     * @return
     */
    private EBusLibClient getLibClient() {

        if (getBridge() == null) {
            throw new RuntimeException("No eBUS bridge defined!");
        }

        EBusBridgeHandler handler = (EBusBridgeHandler) getBridge().getHandler();
        if (handler != null) {
            return handler.getLibClient();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandler#thingUpdated(org.eclipse.smarthome.core.thing.Thing)
     */
    @Override
    public void thingUpdated(final Thing thing) {
        super.thingUpdated(thing);
        initializePolling();
    }

    /**
     *
     */
    private void initializePolling() {

        // cancel all old pollings if available
        cancelPollingJobs();

        final EBusLibClient libClient = getLibClient();

        for (final Channel channel : thing.getChannels()) {

            Configuration configuration = channel.getConfiguration();
            Object object = configuration.get(POLLING);

            final String commandId = channel.getProperties().get(COMMAND);

            // a valid value for polling
            if (object instanceof Number) {
                long pollingPeriod = ((Number) object).longValue();
                try {
                    // compose the telegram
                    final ByteBuffer telegram = libClient.generatePollingTelegram(commandId,
                            IEBusCommandMethod.Method.GET, thing);
                    if (telegram != null) {

                        // random execution delay to prevent too many pollings at the same time (0-30s)
                        int firstExecutionDelay = random.nextInt(30000);

                        // create a job to send this raw telegram every n seconds
                        ScheduledFuture<?> job = scheduler.scheduleAtFixedRate(() -> {
                            logger.info("Poll command {} with {} ...", channel.getUID(),
                                    EBusUtils.toHexDumpString(telegram).toString());

                            libClient.getClient().getController().addToSendQueue(EBusUtils.toByteArray(telegram));

                        }, firstExecutionDelay, pollingPeriod, TimeUnit.SECONDS);

                        // add this job to global list, so we can stop all later on.
                        pollings.put(channel.getUID().getId(), job);
                    }

                } catch (EBusTypeException e) {
                    logger.error("error!", e);
                }

            }
        }
    }

    /**
     * @param sourceAddress
     * @param targetAddress
     * @return
     */
    public boolean supportsTelegram(byte[] receivedData) {

        Byte masterAddress = EBusUtils.toByte((String) getThing().getConfiguration().get(MASTER_ADDRESS));
        Byte slaveAddress = EBusUtils.toByte((String) getThing().getConfiguration().get(SLAVE_ADDRESS));

        // only interesting for broadcasts
        Byte masterAddressComp = masterAddress == null ? EBusUtils.getMasterAddress(slaveAddress) : masterAddress;

        boolean filterAcceptMaster = (boolean) getThing().getConfiguration().get(FILTER_ACCEPT_MASTER);
        boolean filterAcceptSlave = (boolean) getThing().getConfiguration().get(FILTER_ACCEPT_SLAVE);
        boolean filterAcceptBroadcast = (boolean) getThing().getConfiguration().get(FILTER_ACCEPT_BROADCAST);

        if (filterAcceptBroadcast && receivedData[1] == EBusConsts.BROADCAST_ADDRESS) {
            if (masterAddressComp != null && receivedData[0] == masterAddressComp) {
                return true;
            }
        }

        if (filterAcceptMaster && masterAddress != null) {
            if (masterAddress == receivedData[0]) {
                return true;
            }
        }

        if (filterAcceptSlave && slaveAddress != null) {
            if (slaveAddress == receivedData[1]) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param commandChannel
     * @param result
     * @param receivedData
     * @param sendQueueId
     */
    public void handleReceivedTelegram(IEBusCommandMethod commandChannel, Map<String, Object> result,
            byte[] receivedData, Integer sendQueueId) {

        logger.info("Handle received command by thing {} with id {} ...", thing.getLabel(), thing.getUID());

        for (Entry<String, Object> resultEntry : result.entrySet()) {

            ChannelUID channelUID = EBusBindingUtils.generateChannelUID(commandChannel.getParent(),
                    resultEntry.getKey(), thing.getUID());

            Channel channel = thing.getChannel(channelUID.getId());

            if (channel == null) {
                logger.debug("Unable to find the channel with channelUID {}", channelUID.getId());
                return;
            }

            assignValueToChannel(channel, resultEntry.getValue());
        }
    }

    /**
     * @param channel
     * @param value
     */
    @SuppressWarnings("null")
    private void assignValueToChannel(@NonNull Channel channel, Object value) {

        if (channel.getAcceptedItemType().equals("Number")) {
            if (value != null) {
                if (value instanceof BigDecimal) {
                    updateState(channel.getUID(), new DecimalType((BigDecimal) value));
                } else {
                    logger.warn("Unexpected datatype {} for channel {} !", value.getClass().getSimpleName(),
                            channel.getChannelTypeUID().getAsString());
                }

            }

        } else if (channel.getAcceptedItemType().equals("String")) {
            if (value instanceof String) {
                updateState(channel.getUID(), new StringType((String) value));
            } else if (value instanceof byte[]) {
                // show bytes as hex string
                updateState(channel.getUID(), new StringType(EBusUtils.toHexDumpString((byte[]) value).toString()));
            }

        } else if (channel.getAcceptedItemType().equals("Switch")) {
            if (value instanceof Boolean) {
                boolean isOn = ((Boolean) value).booleanValue();
                updateState(channel.getUID(), isOn ? OnOffType.ON : OnOffType.OFF);
            }

        } else if (channel.getAcceptedItemType().equals("DateTime")) {
            if (value instanceof EBusDateTime) {
                this.updateState(channel.getUID(), new DateTimeType(((EBusDateTime) value).getCalendar()));
            }

        }
    }
}
