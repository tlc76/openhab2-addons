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

import org.apache.commons.lang.StringUtils;
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
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.internal.EBusBindingUtils;
import org.openhab.binding.ebus.internal.EBusLibClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.IEBusCommandCollection;
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

    public void refreshThingConfiguration() {

        EBusLibClient libClient = getLibClient();

        Map<@NonNull String, String> properties = thing.getProperties();
        String oldHash = properties.get("collectionHash");
        String collectionId = properties.get(EBusBindingConstants.COLLECTION);
        IEBusCommandCollection collection = libClient.getClient().getCommandCollection(collectionId);

        if (StringUtils.isEmpty(collectionId)) {
            logger.error("Property \"collectionId\" not set for thing {}. Please re-create this thing.",
                    thing.getUID());
            return;
        }

        if (collection == null) {
            logger.error(
                    "Unable to find configuration collection with id {}. It is possible that this collection has been renamed or removed from eBUS binding!",
                    collectionId);
            return;
        }

        // new hash
        String newHash = EBusUtils.toHexDumpString(collection.getSourceHash()).toString();

        // check both hashs
        if (!StringUtils.equals(oldHash, newHash)) {
            logger.warn("eBUS configuration \"{}\"  has changed, update thing {} ...", collection.getId(),
                    thing.getUID());

            try {
                // just update the thing
                this.changeThingType(this.thing.getThingTypeUID(), this.thing.getConfiguration());

                // add the new hash
                this.updateProperty("collectionHash", newHash);

            } catch (RuntimeException e) { // NOPMD - this surely will never happen
                logger.warn(e.getMessage()); // NOPMD - this surely will never happen
            }
        }
    }

    // public synchronized boolean updateThingConfiguration(EBusTypeProvider typeProvider) {
    //
    // logger.info("updateThingConfiguration...");
    //
    // ThingType thingType = typeProvider.getThingType(thing.getThingTypeUID(), null);
    // if (thingType != null) {
    // logger.info("Found channel type {} ...", thingType);
    // } else {
    // logger.warn("Unable to find thing type {} !", thing.getThingTypeUID());
    // }
    //
    // for (Channel channel : thing.getChannels()) {
    // ChannelType channelType = typeProvider.getChannelType(channel.getChannelTypeUID(), null);
    // if (channelType != null) {
    // logger.info("Found channel type {} ...", channelType);
    // } else {
    // logger.warn("Unable to find channel type {} !", channel.getChannelTypeUID());
    // }
    //
    // }
    //
    // return false;
    // }

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

            refreshThingConfiguration();

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

    // /*
    // * (non-Javadoc)
    // *
    // * @see
    // * org.eclipse.smarthome.core.thing.binding.BaseThingHandler#thingUpdated(org.eclipse.smarthome.core.thing.Thing)
    // */
    // @Override
    // public void thingUpdated(final Thing thing) {
    // super.thingUpdated(thing);
    //
    // // reinizialized pollings
    // initializePolling();
    //
    // refreshThingConfiguration();
    //
    // }

    /**
     *
     */
    private void initializePolling() {

        logger.info("Initialize eBUS pollings for {} ...", thing.getUID());

        // cancel all old pollings if available
        cancelPollingJobs();

        final EBusLibClient libClient = getLibClient();

        for (final Channel channel : thing.getChannels()) {

            Configuration configuration = channel.getConfiguration();
            Object pollingObj = configuration.get(POLLING);

            final String collectionId = channel.getProperties().get(COLLECTION);
            final String commandId = channel.getProperties().get(COMMAND);

            // a valid value for polling
            if (pollingObj instanceof Number) {

                long pollingPeriod = ((Number) pollingObj).longValue();

                try {
                    // compose the telegram
                    final ByteBuffer telegram = libClient.generatePollingTelegram(collectionId, commandId,
                            IEBusCommandMethod.Method.GET, thing);
                    if (telegram != null) {

                        // random execution delay to prevent too many pollings at the same time (0-30s)
                        int firstExecutionDelay = random.nextInt(30);

                        // create a job to send this raw telegram every n seconds
                        ScheduledFuture<?> job = scheduler.scheduleAtFixedRate(() -> {
                            logger.info("Poll command \"{}\" with \"{}\" ...", channel.getUID(),
                                    EBusUtils.toHexDumpString(telegram).toString());

                            libClient.getClient().addToSendQueue(EBusUtils.toByteArray(telegram), 2);

                        }, firstExecutionDelay, pollingPeriod, TimeUnit.SECONDS);

                        // add this job to global list, so we can stop all later on.
                        pollings.put(channel.getUID().getId(), job);

                        logger.info("Register polling for \"{}\" every {} sec. (initil delay {} sec.)", commandId,
                                pollingPeriod, firstExecutionDelay);
                    } else {
                        logger.info("Unable to create raw polling telegram for \"{}\" !", commandId);
                    }

                } catch (EBusTypeException e) {
                    logger.error("error!", e);
                }

            }
        }
    }

    /**
     * @param commandMethod
     * @param sourceAddress
     * @param targetAddress
     * @return
     */
    public boolean supportsTelegram(byte[] receivedData, IEBusCommandMethod commandMethod) {

        Configuration configuration = getThing().getConfiguration();

        byte sourceAddress = receivedData[0];
        byte destinationAddress = receivedData[1];

        Byte masterAddress = EBusUtils.toByte((String) configuration.get(MASTER_ADDRESS));
        Byte slaveAddress = EBusUtils.toByte((String) configuration.get(SLAVE_ADDRESS));

        // only interesting for broadcasts
        Byte masterAddressComp = masterAddress == null ? EBusUtils.getMasterAddress(slaveAddress) : masterAddress;

        boolean filterAcceptSource = (boolean) getThing().getConfiguration().get(FILTER_ACCEPT_MASTER);
        boolean filterAcceptDestination = (boolean) getThing().getConfiguration().get(FILTER_ACCEPT_SLAVE);
        boolean filterAcceptBroadcast = (boolean) getThing().getConfiguration().get(FILTER_ACCEPT_BROADCAST);

        String collectionId = thing.getProperties().get(COLLECTION);

        if (!commandMethod.getParent().getParentCollection().getId().equals(collectionId)) {
            logger.trace("eBUS node handler doesn't support this collection id ...");
            return false;
        }

        // check if broadcast filter is set (default true)
        if (filterAcceptBroadcast && destinationAddress == EBusConsts.BROADCAST_ADDRESS) {
            if (masterAddressComp != null && sourceAddress == masterAddressComp) {
                return true;
            }
        }

        // check if source address filter is set
        if (filterAcceptSource && masterAddress != null) {
            if (masterAddress == sourceAddress) {
                return true;
            }
        }

        // check if destination address filter is set (default true)
        if (filterAcceptDestination && slaveAddress != null) {

            if (EBusUtils.isMasterAddress(destinationAddress) && masterAddressComp != null
                    && destinationAddress == masterAddressComp) {
                // master-master telegram
                return true;
            } else if (slaveAddress == destinationAddress) {
                // master-slave telegram
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

            logger.debug("Value {} {}", resultEntry.getKey(), resultEntry.getValue());

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

            if (value instanceof BigDecimal) {
                updateState(channel.getUID(), new StringType(((BigDecimal) value).toString()));

            } else if (value instanceof String) {
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
