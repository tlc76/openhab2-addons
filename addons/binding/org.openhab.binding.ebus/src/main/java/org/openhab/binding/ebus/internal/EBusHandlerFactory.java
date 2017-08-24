/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.EBusBindingConstants.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.handler.EBusHandler;
import org.openhab.binding.ebus.thing.EBusGenerator;
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

    private final Logger logger = LoggerFactory.getLogger(EBusHandlerFactory.class);

    private EBusGenerator generator;

    private EBusClientConfiguration configuration;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return BINDING_ID.equals(thingTypeUID.getBindingId());
    }

    public void setGenerator(EBusGenerator generator) {
        this.generator = generator;

    }

    public void unsetGenerator(EBusGenerator generator) {
        this.generator = null;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_EBUS_BRIDGE)) {
            return new EBusBridgeHandler((Bridge) thing, configuration);
        }

        return new EBusHandler(thing);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);

        configuration = new EBusClientConfiguration();
        updateConfiguration(componentContext.getProperties());
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        super.deactivate(componentContext);
        configuration.clear();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        updateConfiguration(properties);
    }

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

        generator.update(configuration.getCollections());
    }

    private void loadConfigurationByUrl(EBusClientConfiguration configuration, String url) {
        try {
            configuration.loadConfiguration(new URL(url).openStream());
        } catch (MalformedURLException e) {
            logger.error("error!", e);
        } catch (IOException e) {
            logger.error("error!", e);
        }
    }
}
