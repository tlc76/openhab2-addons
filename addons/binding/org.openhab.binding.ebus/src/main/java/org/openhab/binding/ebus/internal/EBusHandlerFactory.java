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

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.handler.EBusHandler;
import org.openhab.binding.ebus.thing.EBusGenerator;

/**
 * The {@link EBusHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusHandlerFactory extends BaseThingHandlerFactory {

    private EBusGenerator generator;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return BINDING_ID.equals(thingTypeUID.getBindingId());
    }

    public void setGenerator(EBusGenerator generator) {
        System.out.println("EBusHandlerFactory.setGenerator()");
        this.generator = generator;

    }

    @Override
    protected Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID) {
        // TODO Auto-generated method stub
        System.out.println("EBusHandlerFactory.createThing()1a");
        return super.createThing(thingTypeUID, configuration, thingUID);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        // TODO Auto-generated method stub
        System.out.println("EBusHandlerFactory.createThing()1b");

        if (configuration.get("device") != null) {
            thingTypeUID = new ThingTypeUID(BINDING_ID, "autotype1");
        }

        return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
    }

    public void unsetGenerator(EBusGenerator generator) {
        this.generator = null;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_EBUS_BRIDGE)) {
            return new EBusBridgeHandler((Bridge) thing, generator);
        }

        return new EBusHandler(thing);
    }
}
