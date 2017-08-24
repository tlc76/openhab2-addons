/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.thing;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ThingType;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusThingTypeProvider implements ThingTypeProvider {

    @Override
    public Collection<ThingType> getThingTypes(Locale locale) {
        System.out.println("EBusThingTypeProvider.getThingTypes()");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ThingType getThingType(ThingTypeUID thingTypeUID, Locale locale) {
        System.out.println("EBusThingTypeProvider.getThingType()");
        // TODO Auto-generated method stub
        return null;
    }

}
