package org.openhab.binding.ebus.thing;

import java.util.Collection;
import java.util.Locale;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ThingType;

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
