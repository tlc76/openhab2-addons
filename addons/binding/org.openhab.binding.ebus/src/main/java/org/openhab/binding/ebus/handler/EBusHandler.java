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

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EBusHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(EBusHandler.class);
    private ScheduledFuture<?> refreshJob;

    public EBusHandler(Thing thing) {
        super(thing);
    }

    private void startAutomaticRefresh() {
        refreshJob = scheduler.scheduleAtFixedRate(() -> {
            try {
                String id = "aa";
                Random rnd = new Random();

                ChannelUID channelUID = new ChannelUID(getThing().getUID(), id + "-" + "deviceId");
                updateState(channelUID, new DecimalType(rnd.nextInt(1000)));

            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            }
        }, 0, 10, TimeUnit.SECONDS);
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
        refreshJob.cancel(true);
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // thingStructureChanged();
            }
        };
        r.run();
        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        startAutomaticRefresh();
    }

    protected void thingStructureChanged() {

        // Prepare properties (convert modes map)
        HashMap<String, String> properties = new HashMap<String, String>();

        // Add node id (for refresh and command handling)
        properties.put("nodeId", "nodeid123");
        properties.put("nodeId", "nodeid123");
        String id = "aa";

        Configuration cfg = new Configuration();
        cfg.put("pattern", "%.1f °C");

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(EBusBindingConstants.BINDING_ID, "temperature");
        ChannelUID channelUID = new ChannelUID(getThing().getUID(), id + "-" + "deviceId");

        Channel channel2 = ChannelBuilder.create(channelUID, "Number").withType(channelTypeUID).withLabel("mylabel")
                .withProperties(properties).build();

        // bindingId:type:thingId:1
        // ChannelUID x = new ChannelUID(EBusBindingConstants.THING_EBUS, "temperature");
        // x = new ChannelUID("ebus:sample:xxx:1");
        // ChannelTypeUID channelTypeUID = new ChannelTypeUID("");

        // Channel channel = ChannelBuilder.create(x, "Number").withType(channelTypeUID).withLabel("DHW Temperature")
        // .withDescription("Meine Beschreibung").build();

        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannel(channel2);

        updateThing(thingBuilder.build());
    }
}
