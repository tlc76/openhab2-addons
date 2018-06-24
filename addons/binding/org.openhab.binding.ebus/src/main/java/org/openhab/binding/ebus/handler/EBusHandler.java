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
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ObjectUtils;
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
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
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

    private Map<ChannelUID, ByteBuffer> channelPollings = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(EBusHandler.class);

    private Random random = new Random(12);

    private Map<ByteBuffer, ScheduledFuture<?>> uniqueTelegramPollings = new HashMap<>();

    /**
     * @param thing
     */
    public EBusHandler(@NonNull Thing thing) {
        super(thing);
    }

    /**
     * Assign a value to a channel.
     *
     * @param channel
     * @param value
     */
    private void assignValueToChannel(@NonNull Channel channel, Object value) {

        String acceptedItemType = channel.getAcceptedItemType();

        if (StringUtils.equals(acceptedItemType, "Number")) {
            if (value != null) {
                if (value instanceof BigDecimal) {
                    updateState(channel.getUID(), new DecimalType((BigDecimal) value));
                } else {
                    logger.warn("Unexpected datatype {} for channel {} !", value.getClass().getSimpleName(),
                            channel.getChannelTypeUID());
                }

            }

        } else if (StringUtils.equals(acceptedItemType, "String")) {

            if (value instanceof BigDecimal) {
                updateState(channel.getUID(), new StringType(((BigDecimal) value).toString()));

            } else if (value instanceof String) {
                updateState(channel.getUID(), new StringType((String) value));

            } else if (value instanceof byte[]) {
                // show bytes as hex string
                updateState(channel.getUID(), new StringType(EBusUtils.toHexDumpString((byte[]) value).toString()));
            }

        } else if (StringUtils.equals(acceptedItemType, "Switch")) {

            if (value instanceof Boolean) {
                boolean isOn = ((Boolean) value).booleanValue();
                updateState(channel.getUID(), isOn ? OnOffType.ON : OnOffType.OFF);
            }

        } else if (StringUtils.equals(acceptedItemType, "DateTime")) {

            if (value instanceof EBusDateTime) {
                Calendar calendar = ((EBusDateTime) value).getCalendar();
                ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(calendar.toInstant(),
                        TimeZone.getDefault().toZoneId());

                this.updateState(channel.getUID(), new DateTimeType(zonedDateTime));
            }

        }
    }

    @Override
    public void channelLinked(@NonNull ChannelUID channelUID) {
        super.channelLinked(channelUID);

        logger.info("channelLinked {}", channelUID);
        initializeChannelPolling(channelUID);
    }

    @Override
    public void channelUnlinked(@NonNull ChannelUID channelUID) {
        super.channelUnlinked(channelUID);

        logger.info("channelUnlinked {}", channelUID);
        disposeChannelPolling(channelUID);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {

        // Cancel all polling jobs
        for (Entry<ByteBuffer, ScheduledFuture<?>> entry : uniqueTelegramPollings.entrySet()) {
            logger.info("Remove polling job for {}", EBusUtils.toHexDumpString(entry.getKey()));
            entry.getValue().cancel(true);
        }

        uniqueTelegramPollings.clear();
        channelPollings.clear();
    }

    /**
     * @param channelUID
     */
    private void disposeChannelPolling(@NonNull ChannelUID channelUID) {

        if (channelPollings.containsKey(channelUID)) {
            ByteBuffer telegram = channelPollings.remove(channelUID);

            if (!channelPollings.containsValue(telegram)) {
                // remove last
                ScheduledFuture<?> future = uniqueTelegramPollings.remove(telegram);
                future.cancel(true);

                logger.debug("Cancel polling job for \"{}\" ...", channelUID);
            } else {
                logger.debug("Polling job still in use for \"{}\" ...", channelUID);
            }
        }
    }

    /**
     * @param channel
     * @return
     */
    private long getChannelPollingInterval(@NonNull Channel channel) {

        final Map<@NonNull String, @NonNull String> properties = channel.getProperties();
        final Configuration thingConfiguration = thing.getConfiguration();
        final Configuration configuration = channel.getConfiguration();

        final String valueName = properties.get(VALUE_NAME);

        // a valid value for polling
        long pollingPeriod = 0;
        if (configuration.get(POLLING) instanceof Number) {
            pollingPeriod = ((Number) configuration.get(POLLING)).longValue();
        }

        // overwrite with global polling if not set
        if (pollingPeriod == 0) {
            // is global polling for this thing enabled?
            if (thingConfiguration.get(POLLING) instanceof Number) {
                pollingPeriod = ((Number) thingConfiguration.get(POLLING)).longValue();
            }
        }

        // skip channels starting with _ , this is a ebus command that starts with _
        if (StringUtils.startsWith(valueName, "_")) {
            pollingPeriod = 0l;
        }

        // only for linked channels
        if (!isLinked(channel.getUID())) {
            pollingPeriod = 0l;
        }

        return pollingPeriod;
    }

    /**
     * Generates the raw telegram for a channel
     *
     * @param channel
     * @return
     */
    private ByteBuffer getChannelTelegram(@NonNull Channel channel) {

        final EBusLibClient libClient = getLibClient();

        final Map<@NonNull String, @NonNull String> properties = channel.getProperties();
        final String collectionId = thing.getThingTypeUID().getId();

        // final String collectionId = properties.get(COLLECTION);
        final String commandId = properties.get(COMMAND);

        try {
            return libClient.generatePollingTelegram(collectionId, commandId, IEBusCommandMethod.Method.GET, thing);

        } catch (EBusTypeException e) {
            logger.error("error!", e);
        }

        return null;
    }

    /**
     * Returns the eBUS core client
     *
     * @return
     */
    private EBusLibClient getLibClient() {

        Bridge bridge = getBridge();
        if (bridge == null) {
            throw new RuntimeException("No eBUS bridge defined!");
        }

        EBusBridgeHandler handler = (EBusBridgeHandler) bridge.getHandler();
        if (handler != null) {
            return handler.getLibClient();
        }

        return null;
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

    /**
     * Processes the received telegram with this handler.
     *
     * @param commandChannel
     * @param result
     * @param receivedData
     * @param sendQueueId
     */
    public void handleReceivedTelegram(IEBusCommandMethod commandChannel, Map<String, Object> result,
            byte[] receivedData, Integer sendQueueId) {

        logger.debug("Handle received command by thing {} with id {} ...", thing.getLabel(), thing.getUID());

        for (Entry<String, Object> resultEntry : result.entrySet()) {

            logger.trace("Key {} with value {}", resultEntry.getKey(), resultEntry.getValue());

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

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge defined!");

        } else if (StringUtils.isEmpty((String) thing.getConfiguration().get(SLAVE_ADDRESS))) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);

        } else if (bridge.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            updateHandler();

        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Slave address is not set!");
        }

    }

    /**
     * Initialize the polling for a channel if used
     *
     * @param channelUID
     */
    private void initializeChannelPolling(@NonNull ChannelUID channelUID) {

        Channel channel = thing.getChannel(channelUID.getId());

        if (channel == null) {
            return;
        }

        long pollingPeriod = getChannelPollingInterval(channel);

        // a valid value for polling ?
        if (pollingPeriod == 0) {
            return;
        }

        final EBusLibClient libClient = getLibClient();
        final String commandId = channel.getProperties().get(COMMAND);

        if (StringUtils.isEmpty(commandId)) {
            logger.warn("Invalid channel uid {}", channelUID);
            logger.warn("Invalid channel {}", channel);
        }

        // compose the telegram
        final ByteBuffer telegram = getChannelTelegram(channel);

        // valid telegram ?
        if (telegram != null) {

            // polling for raw telegram already active?
            if (!channelPollings.containsValue(telegram)) {

                // random execution delay to prevent too many pollings at the same time (0-30s)
                int firstExecutionDelay = random.nextInt(30);

                // create a job to send this raw telegram every n seconds
                ScheduledFuture<?> job = scheduler.scheduleAtFixedRate(() -> {
                    logger.info("Poll command \"{}\" with \"{}\" ...", channel.getUID(),
                            EBusUtils.toHexDumpString(telegram).toString());

                    libClient.getClient().addToSendQueue(EBusUtils.toByteArray(telegram), 2);

                }, firstExecutionDelay, pollingPeriod, TimeUnit.SECONDS);

                // add this job to global list, so we can stop all later on.
                uniqueTelegramPollings.put(telegram, job);

                logger.info("Register polling for \"{}\" every {} sec. (initial delay {} sec.)", commandId,
                        pollingPeriod, firstExecutionDelay);
            } else {

                logger.info("Raw telegram already in use for polling, skip addition polling for \"{}\"!",
                        channel.getUID());
            }

            channelPollings.put(channel.getUID(), telegram);

        } else {
            logger.info("Unable to create raw polling telegram for \"{}\" !", commandId);
        }

    }

    /**
     * Refreshes the thing configuration
     *
     * @return
     */
    public boolean refreshThingConfiguration() {

        EBusLibClient libClient = getLibClient();

        Map<@NonNull String, String> properties = thing.getProperties();
        String oldHash = properties.get("collectionHash");
        String collectionId = thing.getThingTypeUID().getId();
        IEBusCommandCollection collection = libClient.getClient().getCommandCollection(collectionId);

        if (StringUtils.isEmpty(collectionId)) {
            logger.error("Property \"collectionId\" not set for thing {}. Please re-create this thing.",
                    thing.getUID());
            return false;
        }

        if (collection == null) {
            logger.error(
                    "Unable to find configuration collection with id {}. It is possible that this collection has been renamed or removed from eBUS binding!",
                    collectionId);
            return false;
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

                return true;

            } catch (RuntimeException e) { // NOPMD - used in the openHAB core
                logger.error("Error: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * Check if this handler supportes the given command. In this case this method returns true.
     *
     * @param receivedData
     * @param commandMethod
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

        String collectionId = thing.getThingTypeUID().getId();

        if (!commandMethod.getParent().getParentCollection().getId().equals(collectionId)) {
            logger.trace("eBUS node handler {} use collectionId {}, not {} ...", thing.getUID(), collectionId,
                    commandMethod.getParent().getParentCollection().getId());
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

    @Override
    public void thingUpdated(@NonNull Thing thing) {

        // Info: I expect no difference in the channel list without a restart!

        logger.warn("EBusHandler.thingUpdated()");

        Thing currentThing = this.thing;

        this.thing = thing;

        for (Channel oldChannel : currentThing.getChannels()) {

            Channel newChannel = thing.getChannel(oldChannel.getUID().getId());
            logger.info("thingUpdated {}", oldChannel.getUID());
            if (newChannel != null
                    && !ObjectUtils.equals(oldChannel.getConfiguration(), newChannel.getConfiguration())) {
                logger.debug("Configuration for channel {} changed from {} to {} ...", oldChannel.getUID(),
                        oldChannel.getConfiguration(), newChannel.getConfiguration());

                logger.info("thingUpdated {}", thing.getUID());
                updateChannelPolling(oldChannel.getUID());
            }
        }
    }

    /**
     * Update a channel and its polling
     *
     * @param channelUID
     */
    private void updateChannelPolling(@NonNull ChannelUID channelUID) {
        disposeChannelPolling(channelUID);
        initializeChannelPolling(channelUID);
    }

    /**
     * Updates the handler incl. all pollings
     */
    public void updateHandler() {

        logger.info("Initialize all eBUS pollings for {} ...", thing.getUID());

        for (final Channel channel : thing.getChannels()) {
            updateChannelPolling(channel.getUID());
        }
    }
}
