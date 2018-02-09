/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.EBusBindingConstants.BINDING_ID;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.handler.EBusHandler;
import org.openhab.binding.ebus.thing.EBusTypeProvider;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EBusHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusHandlerFactory extends BaseThingHandlerFactory {

    @SuppressWarnings("unused")
    private final Logger logger = LoggerFactory.getLogger(EBusHandlerFactory.class);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private EBusTypeProvider typeProvider;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#activate(org.osgi.service.component.
     * ComponentContext)
     */
    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#createHandler(org.eclipse.smarthome.core.thing.
     * Thing)
     */
    @Override
    protected ThingHandler createHandler(Thing thing) {

        if (EBusBridgeHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            return new EBusBridgeHandler((Bridge) thing, typeProvider, this);

        } else if (BINDING_ID.equals(thing.getUID().getBindingId())) {
            return new EBusHandler(thing);

        } else {
            return null;

        }
    }

    /**
     * @param bridgeHandler
     */
    public synchronized void registerDiscoveryService(@NonNull EBusBridgeHandler bridgeHandler) {
        EBusDiscovery discoveryService = new EBusDiscovery(bridgeHandler);

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put("service.pid", "discovery.ebus");

        ServiceRegistration<?> service = bundleContext.registerService(DiscoveryService.class.getName(),
                discoveryService, hashtable);

        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), service);

        discoveryService.activate();
    }

    public synchronized void disposeDiscoveryService(EBusBridgeHandler bridgeHandler) {

        ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(bridgeHandler.getThing().getUID());

        if (serviceReg != null) {

            // remove discovery service
            EBusDiscovery service = (EBusDiscovery) bundleContext.getService(serviceReg.getReference());

            if (service != null) {
                service.deactivate();
            }

            serviceReg.unregister();
            discoveryServiceRegs.remove(bridgeHandler.getThing().getUID());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#removeHandler(org.eclipse.smarthome.core.thing.
     * binding.ThingHandler)
     */
    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof EBusBridgeHandler) {
            disposeDiscoveryService((EBusBridgeHandler) thingHandler);
        }
    }

    public void setTypeProvider(EBusTypeProvider typeProvider) {
        this.typeProvider = typeProvider;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory#supportsThingType(org.eclipse.smarthome.core.thing.
     * ThingTypeUID)
     */
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return BINDING_ID.equals(thingTypeUID.getBindingId());
    }

    /**
     * @param typeProvider
     */
    public void unsetTypeProvider(EBusTypeProvider typeProvider) {
        this.typeProvider = null;
    }
}
