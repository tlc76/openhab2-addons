package org.openhab.binding.ebus.thing;

import static org.openhab.binding.ebus.EBusBindingConstants.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;

import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusValue;

public class EBusGeneratorBase implements ThingTypeProvider, ChannelTypeProvider, ConfigDescriptionProvider {

    protected Collection<ChannelGroupType> channelGroupTypes = new ArrayList<>();

    protected Collection<ChannelType> channelTypes = new ArrayList<>();

    protected Collection<ConfigDescription> configDescriptions = new ArrayList<>();

    protected final List<String> supportedBridgeTypeUIDs = Arrays.asList(THING_TYPE_EBUS_BRIDGE.getAsString());

    protected Collection<ThingType> thingTypes = new ArrayList<>();

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        for (ChannelGroupType channelGroupType : channelGroupTypes) {
            if (channelGroupType.getUID().equals(channelGroupTypeUID)) {
                return channelGroupType;
            }
        }

        return null;
    }

    protected ChannelTypeUID generateChannelTypeUID(IEBusCommand command, IEBusValue value) {
        return new ChannelTypeUID(BINDING_ID + ":" + command.getId().replace('.', ':') + ":" + value.getName());
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        return Collections.unmodifiableCollection(channelGroupTypes);
    }

    protected ChannelGroupTypeUID generateChannelGroupTypeUID(IEBusCommand command) {
        return new ChannelGroupTypeUID(BINDING_ID + ":" + command.getId().replace('.', '-'));
    }

    @Override
    public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
        for (ChannelType channelType : channelTypes) {
            if (channelType.getUID().equals(channelTypeUID)) {
                return channelType;
            }
        }

        return null;
    }

    @Override
    public Collection<ChannelType> getChannelTypes(Locale locale) {
        return Collections.unmodifiableCollection(channelTypes);
    }

    @Override
    public ConfigDescription getConfigDescription(URI uri, Locale locale) {
        for (ConfigDescription configDescription : configDescriptions) {
            if (configDescription.getUID().equals(uri)) {
                return configDescription;
            }
        }
        return null;
    }

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(Locale locale) {
        return configDescriptions;
    }

    @Override
    public ThingType getThingType(ThingTypeUID thingTypeUID, Locale locale) {
        for (ThingType thingType : thingTypes) {
            if (thingType.getUID().equals(thingTypeUID)) {
                return thingType;
            }
        }

        return null;
    }

    @Override
    public Collection<ThingType> getThingTypes(Locale locale) {
        return thingTypes;
    }
}
