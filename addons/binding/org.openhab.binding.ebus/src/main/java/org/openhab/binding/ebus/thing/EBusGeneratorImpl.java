package org.openhab.binding.ebus.thing;

import static org.openhab.binding.ebus.EBusBindingConstants.CONFIG_DESCRIPTION_URI_THING;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusGeneratorImpl
        implements EBusGenerator, ThingTypeProvider, ChannelTypeProvider, ConfigDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(EBusGeneratorImpl.class);

    private final List<String> supportedBridgeTypeUIDs = Arrays
            .asList(EBusBindingConstants.THING_TYPE_EBUS_BRIDGE.getAsString());

    private Collection<ThingType> thingTypes = new ArrayList<>();
    private Collection<ConfigDescription> configDescriptions = new ArrayList<>();

    public EBusGeneratorImpl() {

        ThingTypeUID thingTypeUID = new ThingTypeUID(EBusBindingConstants.BINDING_ID, "mythingtype");
        URI m = getConfigDescriptionURI(thingTypeUID);

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Thing.PROPERTY_VENDOR, "Wolf GmbH");
        properties.put(Thing.PROPERTY_MODEL_ID, "Solar SM1 Module");
        properties.put(Thing.PROPERTY_HARDWARE_VERSION, "3.4.1");
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, "1.60");

        properties.put("slaveAddress", "0x05");
        properties.put("masterAddress", "0xFF");

        // config
        List<ConfigDescriptionParameter> parms = new ArrayList<ConfigDescriptionParameter>();

        ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder.create("uuu", Type.TEXT);
        builder.withDescription("My description");
        builder.withLabel("My Label");
        builder.withReadOnly(false);
        parms.add(builder.build());

        ConfigDescription x = new ConfigDescription(m, parms);

        configDescriptions.add(x);

        ThingType t = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, "MyType", "My desc", null, null, properties,
                m);

        thingTypes.add(t);
    }

    private URI getConfigDescriptionURI(ThingTypeUID thingTypeUID) {
        try {
            return new URI(String.format("%s:%s", CONFIG_DESCRIPTION_URI_THING, thingTypeUID.getAsString()));
        } catch (URISyntaxException ex) {
            logger.warn("Can't create configDescriptionURI for device type {}", thingTypeUID.getAsString());
            return null;
        }
    }

    @Override
    public Collection<ThingType> getThingTypes(Locale locale) {
        return thingTypes;
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
    public Collection<ChannelType> getChannelTypes(Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(Locale locale) {
        return configDescriptions;
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

}
