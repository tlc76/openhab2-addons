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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.internal.EBusLibClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.cfg.datatypes.EBusTypeException;
import de.csdev.ebus.command.IEBusCommandMethod;
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

    public EBusHandler(@NonNull Thing thing) {
        super(thing);
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
    public void dispose() {
        cancelPollingJobs();
        // refreshJob.cancel(true);
    }

    private void cancelPollingJobs() {
        for (ScheduledFuture<?> pollingJob : pollings.values()) {
            pollingJob.cancel(true);
        }
        pollings.clear();
    }

    @Override
    public void initialize() {

        updateStatus(ThingStatus.ONLINE);

        initializePolling();
    }

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

    @Override
    public void thingUpdated(final Thing thing) {
        super.thingUpdated(thing);

        initializePolling();
    }

    private void initializePolling() {
        cancelPollingJobs();

        final EBusLibClient libClient = getLibClient();

        for (final Channel channel : thing.getChannels()) {
            Configuration configuration = channel.getConfiguration();
            Object object = configuration.get(CONFIG_POLLING);

            final String commandId = channel.getProperties().get(EBusBindingConstants.PROPERTY_COMMAND);

            if (object instanceof Number) {
                long pollingPeriod = ((Number) object).longValue();
                try {
                    final ByteBuffer telegram = libClient.grrr(commandId, IEBusCommandMethod.Method.GET, thing);
                    if (telegram != null) {
                        pollings.put(channel.getUID().getId(), scheduler.scheduleAtFixedRate(() -> {
                            logger.info("Poll command {} with {} ...", channel.getUID(),
                                    EBusUtils.toHexDumpString(telegram).toString());

                            libClient.getClient().getController().addToSendQueue(telegram);

                        }, 0, pollingPeriod, TimeUnit.SECONDS));
                    }

                } catch (EBusTypeException e) {
                    logger.error("error!", e);
                }

            }
        }
    }
}
