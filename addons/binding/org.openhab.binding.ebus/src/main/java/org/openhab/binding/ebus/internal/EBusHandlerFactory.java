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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClientConfiguration;

/**
 * The {@link EBusHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusHandlerFactory extends BaseThingHandlerFactory implements ManagedService {

    private EBusClientConfiguration configuration;

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private EBusTypeProvider typeProvider;

    private final Logger logger = LoggerFactory.getLogger(EBusHandlerFactory.class);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#activate(org.osgi.service.component.
     * ComponentContext)
     */
    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);

        configuration = new EBusClientConfiguration();
        updateConfiguration(componentContext.getProperties());
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
            EBusBridgeHandler handler = new EBusBridgeHandler((Bridge) thing, configuration);
            registerDiscoveryService(handler);
            return handler;

        } else if (BINDING_ID.equals(thing.getUID().getBindingId())) {
            return new EBusHandler(thing);

        } else {
            return null;

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory#deactivate(org.osgi.service.component.
     * ComponentContext)
     */
    @Override
    protected void deactivate(ComponentContext componentContext) {
        super.deactivate(componentContext);
        configuration.clear();
    }

    /**
     * @param configuration
     * @param url
     */
    private void loadConfigurationByUrl(EBusClientConfiguration configuration, String url) {
        try {
            configuration.loadConfiguration(new URL(url).openStream());
        } catch (MalformedURLException e) {
            logger.error("error!", e);
        } catch (IOException e) {
            logger.error("error!", e);
        }
    }

    /**
     * @param bridgeHandler
     */
    private synchronized void registerDiscoveryService(EBusBridgeHandler bridgeHandler) {
        EBusDiscovery discoveryService = new EBusDiscovery(bridgeHandler);
        discoveryService.activate();

        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
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
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                EBusDiscovery service = (EBusDiscovery) bundleContext.getService(serviceReg.getReference());
                if (service != null) {
                    service.deactivate();
                }

                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    /**
     * @param typeProvider
     */
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

    /**
     * @param properties
     */
    private void updateConfiguration(Dictionary<String, ?> properties) {

        configuration.clear();

        configuration.loadInternalConfigurations();

        if (properties.get("configurationUrl") instanceof String) {
            loadConfigurationByUrl(configuration, (String) properties.get("configurationUrl"));
        }
        if (properties.get("configurationUrl1") instanceof String) {
            loadConfigurationByUrl(configuration, (String) properties.get("configurationUrl1"));
        }
        if (properties.get("configurationUrl2") instanceof String) {
            loadConfigurationByUrl(configuration, (String) properties.get("configurationUrl2"));
        }

        typeProvider.update(configuration.getCollections());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null) {
            updateConfiguration(properties);
        }
    }
}
