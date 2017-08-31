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
import de.csdev.ebus.command.EBusCommandCollection;
import de.csdev.ebus.service.device.EBusDeviceTableListener;
import de.csdev.ebus.service.device.IEBusDevice;
import de.csdev.ebus.utils.EBusUtils;

public class EBusDiscovery extends AbstractDiscoveryService implements EBusDeviceTableListener {

    private final Logger logger = LoggerFactory.getLogger(EBusDiscovery.class);

    private EBusBridgeHandler bridgeHandle;

    public EBusDiscovery(EBusBridgeHandler bridgeHandle) throws IllegalArgumentException {
        super(60);

        this.bridgeHandle = bridgeHandle;
        bridgeHandle.getLibClient().getClient().getDeviceTable().addEBusDeviceTableListener(this);

        System.err.println("EBusDiscovery.EBusDiscovery()");
    }

    @Override
    protected void startScan() {
        System.err.println("EBusDiscovery.startScan()");
    }

    protected void activate() {
        super.activate(new HashMap<String, Object>());
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
        bridgeHandle.getLibClient().getClient().getDeviceTable().removeEBusDeviceTableListener(this);
    }

    private void x(IEBusDevice device, EBusCommandCollection collection) {

        String masterAddress = EBusUtils.toHexDumpString(device.getMasterAddress());
        String slaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

        String id = collection.getId() + "_" + masterAddress + "_" + slaveAddress;

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, collection.getId());
        ThingUID thingUID = new ThingUID(BINDING_ID, id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(EBusBindingConstants.CONFIG_MASTER_ADDRESS, masterAddress);
        properties.put(EBusBindingConstants.CONFIG_SLAVE_ADDRESS, slaveAddress);

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

            EBusClient client = bridgeHandle.getLibClient().getClient();
            Collection<EBusCommandCollection> commandCollections = client.getConfigurationProvider()
                    .getCommandCollections();
            logger.warn("EBusDiscovery.onEBusDeviceUpdate()");

            x(device, client.getConfigurationProvider().getCommandCollection("common"));

            String deviceStr = EBusUtils.toHexDumpString(device.getDeviceId()).toString();
            for (final EBusCommandCollection collection : commandCollections) {
                if (deviceStr.equals(collection.getIdentification())) {
                    logger.info("Discovered device {} ...", collection.getId());
                    x(device, collection);
                }
            }

            //
            // String masterAddress = EBusUtils.toHexDumpString(device.getMasterAddress());
            // String slaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());
            //
            // String id = "common" + "_" + masterAddress + "_" + slaveAddress;
            //
            // ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, "common");
            // ThingUID thingUID = new ThingUID(BINDING_ID, id);
            //
            // Map<String, Object> properties = new HashMap<>();
            //
            // properties.put(EBusBindingConstants.CONFIG_MASTER_ADDRESS, masterAddress);
            // properties.put(EBusBindingConstants.CONFIG_SLAVE_ADDRESS, slaveAddress);
            //
            // properties.put("vendor", "Wolf");
            // properties.put("hardwareVersion", device.getHardwareVersion().toPlainString());
            // properties.put("softwareVersion", device.getSoftwareVersion().toPlainString());
            //
            // if (device.getDeviceId() != null) {
            // String deviceStr = EBusUtils.toHexDumpString(device.getDeviceId()).toString();
            // List<EBusCommandCollection> commandCollections = client.getConfigurationProvider()
            // .getCommandCollections();
            //
            // for (EBusCommandCollection collection : commandCollections) {
            // if (deviceStr.equals(collection.getIdentification())) {
            // logger.info("Discovered device {} ...", collection.getId());
            //
            // String xid = "common" + "_" + masterAddress + "_" + slaveAddress;
            //
            // DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
            // .withThingType(thingTypeUID).withProperties(properties)
            // .withBridge(bridgeHandle.getThing().getUID())
            // .withRepresentationProperty("eBUS Standard " + masterAddress + " " + slaveAddress)
            // .withLabel("eBUS Standard " + masterAddress + " " + slaveAddress).build();
            //
            // thingRemoved(thingUID);
            // thingDiscovered(discoveryResult);
            //
            // }
            // }
            // }
            //
            // // device.getManufacturer()
            //
            // // properties.put("", value)
            // // device.getMasterAddress()
            //
            // // if (type.equals(TYPE.NEW)) {
            //
            // DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
            // .withProperties(properties).withBridge(bridgeHandle.getThing().getUID())
            // .withRepresentationProperty("eBUS Standard " + masterAddress + " " + slaveAddress)
            // .withLabel("eBUS Standard " + masterAddress + " " + slaveAddress).build();
            //
            // thingRemoved(thingUID);
            // thingDiscovered(discoveryResult);
            //
            // // }
        }

    }

}
