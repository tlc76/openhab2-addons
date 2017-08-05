package org.openhab.binding.ebus.thing;

import static org.openhab.binding.ebus.EBusBindingConstants.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.types.EventDescription;
import org.eclipse.smarthome.core.types.StateDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusGeneratorImpl
        implements EBusGenerator, ThingTypeProvider, ChannelTypeProvider, ConfigDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(EBusGeneratorImpl.class);

    private final List<String> supportedBridgeTypeUIDs = Arrays.asList(THING_TYPE_EBUS_BRIDGE.getAsString());

    private Collection<ThingType> thingTypes = new ArrayList<>();
    private Collection<ConfigDescription> configDescriptions = new ArrayList<>();

    private Collection<ChannelType> channelTypes = new ArrayList<>();
    private Collection<ChannelGroupType> channelGroupTypes = new ArrayList<>();

    public EBusGeneratorImpl() {

        // ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, "mythingtype");
        // URI m = getConfigDescriptionURI(thingTypeUID);
        //
        // Map<String, String> properties = new HashMap<String, String>();
        // properties.put(Thing.PROPERTY_VENDOR, "Wolf GmbH");
        // properties.put(Thing.PROPERTY_MODEL_ID, "Solar SM1 Module");
        // properties.put(Thing.PROPERTY_HARDWARE_VERSION, "3.4.1");
        // properties.put(Thing.PROPERTY_FIRMWARE_VERSION, "1.60");
        //
        // properties.put("slaveAddress", "0x05");
        // properties.put("masterAddress", "0xFF");
        //
        // // config
        // List<ConfigDescriptionParameter> parms = new ArrayList<ConfigDescriptionParameter>();
        //
        // ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder.create("uuu", Type.TEXT);
        // builder.withDescription("My description");
        // builder.withLabel("My Label");
        // builder.withReadOnly(false);
        // parms.add(builder.build());
        //
        // Channel build = ChannelBuilder.create(new ChannelUID("bindingId:type:thingId:1"), "String").build();
        //
        // ConfigDescription x = new ConfigDescription(m, parms);
        //
        // configDescriptions.add(x);
        //
        // ThingType t = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, "MyType", "My desc", null, null,
        // properties,
        // m);

        ThingType thingType = createThingType();
        thingTypes.add(thingType);

        ChannelType channelType = createChannelType();
        channelTypes.add(channelType);

        ChannelGroupType channelGroupType = createChannelGroupType();
        channelGroupTypes.add(channelGroupType);
    }

    private ThingType createThingType() {

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, "mythingtype");

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(Thing.PROPERTY_VENDOR, "Wolf GmbH");
        properties.put(Thing.PROPERTY_MODEL_ID, "Solar SM1 Module");
        properties.put(Thing.PROPERTY_HARDWARE_VERSION, "3.4.1");
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, "1.60");
        properties.put("slaveAddress", "0x05");
        properties.put("masterAddress", "0xFF");

        String label = "MyType";
        String description = "My desc";
        // List<ChannelDefinition> channelDefinitions = Arrays
        // .asList(new ChannelDefinition("cdfID", getChannelTypeUNID("channel5")));
        List<ChannelDefinition> channelDefinitions = null;
        List<ChannelGroupDefinition> channelGroupDefinitions = Arrays
                .asList(new ChannelGroupDefinition("id", getChannelGroupTypeUNID("boilerGroupType")));
        URI configDescriptionURI = null;

        ThingType thingType = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description,
                channelDefinitions, channelGroupDefinitions, properties, configDescriptionURI);

        return thingType;
    }

    @SuppressWarnings("deprecation")
    private ChannelGroupType createChannelGroupType() {

        ChannelDefinition cd = new ChannelDefinition("id", getChannelTypeUNID("channel5"), null, "label",
                "description");

        List<ChannelDefinition> asList = Arrays.asList(cd);

        ChannelGroupType cgt = new ChannelGroupType(getChannelGroupTypeUNID("boilerGroupType"), false, "label",
                "description", asList);

        return cgt;
    }

    private ChannelType createChannelType() {

        ChannelTypeUID uid = getChannelTypeUNID("channel5");
        boolean advanced = false;
        String itemType = "Number";
        ChannelKind kind = ChannelKind.STATE;
        String label = "MyLabel";
        String description = "My Description";
        String category = "CAT";
        Set<String> tags = null;
        StateDescription state = null;
        EventDescription event = null;
        URI configDescriptionURI = null;

        return new ChannelType(uid, advanced, itemType, kind, label, description, category, tags, state, event,
                configDescriptionURI);
    }

    private ChannelGroupTypeUID getChannelGroupTypeUNID(String id) {
        return new ChannelGroupTypeUID(BINDING_ID, id);
    }

    private ChannelTypeUID getChannelTypeUNID(String id) {
        return new ChannelTypeUID(BINDING_ID, id);
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
        return Collections.unmodifiableCollection(channelTypes);
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
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        for (ChannelGroupType channelGroupType : channelGroupTypes) {
            if (channelGroupType.getUID().equals(channelGroupTypeUID)) {
                return channelGroupType;
            }
        }

        return null;
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        return Collections.unmodifiableCollection(channelGroupTypes);
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
