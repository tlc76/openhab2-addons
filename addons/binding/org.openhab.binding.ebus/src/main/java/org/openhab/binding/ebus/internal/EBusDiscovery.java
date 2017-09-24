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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.IEBusCommandCollection;
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

    public EBusDiscovery(EBusBridgeHandler bridgeHandle) throws IllegalArgumentException {
        super(60);

        this.bridgeHandle = bridgeHandle;
        bridgeHandle.getLibClient().getClient().addEBusDeviceTableListener(this);
    }

    @Override
    protected void startScan() {
        logger.debug("Starting eBUS discovery scan ...");
        //
        // EBusClient client = bridgeHandle.getLibClient().getClient();
        // client.getDeviceTableService().startDeviceScan();

    }

    protected void activate() {
        super.activate(new HashMap<String, Object>());
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

    private void updateDiscoveredThing(IEBusDevice device, IEBusCommandCollection collection) {

        String masterAddress = EBusUtils.toHexDumpString(device.getMasterAddress());
        String slaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

        String id = collection.getId() + "_" + masterAddress + "_" + slaveAddress;

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, collection.getId());
        ThingUID thingUID = new ThingUID(BINDING_ID, id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(EBusBindingConstants.MASTER_ADDRESS, masterAddress);
        properties.put(EBusBindingConstants.SLAVE_ADDRESS, slaveAddress);

        if (device.getHardwareVersion() != null) {
            properties.put("hardwareVersion", device.getHardwareVersion().toPlainString());
        }

        if (device.getSoftwareVersion() != null) {
            properties.put("softwareVersion", device.getSoftwareVersion().toPlainString());
        }

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                .withProperties(properties).withBridge(bridgeHandle.getThing().getUID())
                .withRepresentationProperty(collection.getLabel() + " " + masterAddress + " " + slaveAddress)
                .withLabel(collection.getLabel() + " " + masterAddress + " " + slaveAddress).build();

        thingRemoved(thingUID);
        thingDiscovered(discoveryResult);

    }

    @Override
    public void onEBusDeviceUpdate(TYPE type, IEBusDevice device) {

        if (!type.equals(TYPE.UPDATE_ACTIVITY)) {

            logger.warn("EBusDiscovery.onEBusDeviceUpdate()");

            EBusClient client = bridgeHandle.getLibClient().getClient();

            Collection<IEBusCommandCollection> commandCollections = client.getCommandCollections();
            IEBusCommandCollection commonCollection = client.getCommandCollection("common");

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
        }
    }
}
