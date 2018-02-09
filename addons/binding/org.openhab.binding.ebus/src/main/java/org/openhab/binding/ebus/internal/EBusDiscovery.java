/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.EBusBindingConstants.BINDING_ID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.core.EBusConsts;
import de.csdev.ebus.service.device.EBusDeviceTableService;
import de.csdev.ebus.service.device.IEBusDevice;
import de.csdev.ebus.service.device.IEBusDeviceTableListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusDiscovery extends AbstractDiscoveryService implements IEBusDeviceTableListener {

    private final Logger logger = LoggerFactory.getLogger(EBusDiscovery.class);

    private EBusBridgeHandler bridgeHandle;

    // private ScheduledFuture<?> discoveryJob;

    public EBusDiscovery(@NonNull EBusBridgeHandler bridgeHandle) throws IllegalArgumentException {
        super(new HashSet<>(Arrays.asList(bridgeHandle.getThing().getThingTypeUID())), 20, false);

        this.bridgeHandle = bridgeHandle;
        bridgeHandle.getLibClient().getClient().addEBusDeviceTableListener(this);
    }

    // @Override
    // protected void startBackgroundDiscovery() {
    // logger.info("Start eBUS device background discovery");
    // if (discoveryJob == null || discoveryJob.isCancelled()) {
    // discoveryJob = scheduler.scheduleWithFixedDelay(new Runnable() {
    //
    // @Override
    // public void run() {
    // EBusDeviceTableService deviceTableService = getDeviceTableService();
    // if (deviceTableService != null) {
    // deviceTableService.inquiryDeviceExistence();
    // }
    // }
    //
    // }, 0, 20, TimeUnit.MINUTES);
    // }
    // }

    /**
     * @return
     */
    private EBusDeviceTableService getDeviceTableService() {
        return bridgeHandle.getLibClient().getClient().getDeviceTableService();
    }

    // @Override
    // protected void stopBackgroundDiscovery() {
    // logger.info("Stop eBUS device background discovery");
    // if (discoveryJob != null && !discoveryJob.isCancelled()) {
    // discoveryJob.cancel(true);
    // discoveryJob = null;
    // }
    // }

    @Override
    protected void startScan() {
        logger.warn("Starting eBUS discovery scan ...");

        EBusDeviceTableService deviceTableService = getDeviceTableService();
        if (deviceTableService != null) {
            deviceTableService.inquiryDeviceExistence();
        }
    }

    protected void activate() {
        super.activate(new HashMap<String, Object>());
        logger.debug("Start eBUS discovery service ...");
    }

    @Override
    public void deactivate() {

        logger.debug("Stopping eBUS discovery service ...");

        removeOlderResults(new Date().getTime());

        try {
            bridgeHandle.getLibClient().getClient().removeEBusDeviceTableListener(this);
        } catch (Exception e) {
            // okay, maybe not set
        }
    }

    /**
     * @param device
     * @param collection
     */
    private void updateDiscoveredThing(IEBusDevice device, IEBusCommandCollection collection) {

        String masterAddress = EBusUtils.toHexDumpString(device.getMasterAddress());
        String slaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

        String id = slaveAddress;

        if (StringUtils.isEmpty(id)) {
            logger.debug("No slave address for device available! {}", device.toString());
            return;
        }

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, collection.getId());
        ThingUID thingUID = new ThingUID(thingTypeUID, bridgeHandle.getThing().getUID(), id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(EBusBindingConstants.MASTER_ADDRESS, masterAddress);
        properties.put(EBusBindingConstants.SLAVE_ADDRESS, slaveAddress);

        // not nice from the api, one time Map<String, String> another time <String, Object>
        Map<String, String> deviceProperties = new HashMap<>();
        updateThingProperties(device, deviceProperties);
        properties.putAll(deviceProperties);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                .withProperties(properties).withBridge(bridgeHandle.getThing().getUID())
                .withRepresentationProperty(String.format("%s (%s)", collection.getLabel(), slaveAddress))
                .withLabel(String.format("%s (%s)", collection.getLabel(), slaveAddress)).build();

        thingDiscovered(discoveryResult);
    }

    @Override
    public void onEBusDeviceUpdate(TYPE type, IEBusDevice device) {

        if (!type.equals(TYPE.UPDATE_ACTIVITY)) {

            EBusClient client = bridgeHandle.getLibClient().getClient();

            Collection<IEBusCommandCollection> commandCollections = client.getCommandCollections();
            IEBusCommandCollection commonCollection = client.getCommandCollection(EBusConsts.COLLECTION_STD);

            // update common thing
            updateDiscoveredThing(device, commonCollection);

            // search for collection with device id
            String deviceStr = EBusUtils.toHexDumpString(device.getDeviceId()).toString();
            for (final IEBusCommandCollection collection : commandCollections) {
                if (collection.getIdentification().contains(deviceStr)) {
                    logger.info("Discovered device {} ...", collection.getId());
                    updateDiscoveredThing(device, collection);
                }
            }

            // update already initialized eBUS nodes
            updateInitializedThings(device);
        }
    }

    /**
     * Update already initialized things
     *
     * @param type
     * @param device
     */
    private void updateInitializedThings(IEBusDevice device) {

        if (bridgeHandle == null || bridgeHandle.getThing() == null || bridgeHandle.getThing().getThings() == null) {
            logger.debug("No things available ...");
            return;
        }

        String deviceSlaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

        for (Thing thing : bridgeHandle.getThing().getThings()) {
            String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);

            if (deviceSlaveAddress.equals(slaveAddress)) {

                Map<String, String> properties = new HashMap<>();
                properties.putAll(thing.getProperties());
                updateThingProperties(device, properties);
                thing.setProperties(properties);
            }
        }
    }

    /**
     * @param device
     * @param properties
     */
    private void updateThingProperties(IEBusDevice device, Map<String, String> properties) {

        if (device.getDeviceId() != null && device.getDeviceId().length == 5) {
            properties.put(Thing.PROPERTY_MODEL_ID, EBusUtils.toHexDumpString(device.getDeviceId()).toString());
        } else {
            properties.remove(Thing.PROPERTY_MODEL_ID);
        }

        if (device.getHardwareVersion() != null) {
            properties.put(Thing.PROPERTY_HARDWARE_VERSION, device.getHardwareVersion().toPlainString());
        } else {
            properties.remove(Thing.PROPERTY_HARDWARE_VERSION);
        }

        if (device.getSoftwareVersion() != null) {
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, device.getSoftwareVersion().toPlainString());
        } else {
            properties.remove(Thing.PROPERTY_FIRMWARE_VERSION);
        }

        if (device.getManufacturerName() != null) {
            properties.put(Thing.PROPERTY_VENDOR, device.getManufacturerName());
        } else {
            properties.remove(Thing.PROPERTY_VENDOR);
        }
    }

}
