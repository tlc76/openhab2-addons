package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.EBusBindingConstants.BINDING_ID;

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

import de.csdev.ebus.service.device.EBusDeviceTableListener;
import de.csdev.ebus.service.device.IEBusDevice;
import de.csdev.ebus.utils.EBusUtils;

public class EBusDiscovery extends AbstractDiscoveryService implements EBusDeviceTableListener {

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

    @Override
    public void onEBusDeviceUpdate(TYPE type, IEBusDevice device) {

        if (!type.equals(TYPE.UPDATE_ACTIVITY)) {

            System.err.println("EBusDiscovery.onEBusDeviceUpdate()");

            String masterAddress = EBusUtils.toHexDumpString(device.getMasterAddress());
            String slaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

            String id = "common" + "_" + masterAddress + "_" + slaveAddress;

            ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, "common");
            ThingUID thingUID = new ThingUID(BINDING_ID, id);

            Map<String, Object> properties = new HashMap<>();

            properties.put(EBusBindingConstants.CONFIG_MASTER_ADDRESS, masterAddress);
            properties.put(EBusBindingConstants.CONFIG_SLAVE_ADDRESS, slaveAddress);
            properties.put("vendor", "Wolf");

            // device.getManufacturer()

            // properties.put("", value)
            // device.getMasterAddress()

            // if (type.equals(TYPE.NEW)) {

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withBridge(bridgeHandle.getThing().getUID())
                    .withRepresentationProperty("eBUS Standard " + masterAddress + " " + slaveAddress)
                    .withLabel("eBUS Standard " + masterAddress + " " + slaveAddress).build();

            thingRemoved(thingUID);
            thingDiscovered(discoveryResult);

            // }
        }

    }

}
