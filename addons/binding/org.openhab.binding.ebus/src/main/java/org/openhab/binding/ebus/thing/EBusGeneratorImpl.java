package org.openhab.binding.ebus.thing;

import static org.openhab.binding.ebus.EBusBindingConstants.BINDING_ID;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.types.EventDescription;
import org.eclipse.smarthome.core.types.StateDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.EBusCommandCollection;
import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusValue;

public class EBusGeneratorImpl extends EBusGeneratorBase implements EBusGenerator {

    private final Logger logger = LoggerFactory.getLogger(EBusGeneratorImpl.class);

    private Random random;

    public EBusGeneratorImpl() {

        // ThingType thingType = createThingType();
        // thingTypes.add(thingType);
        //
        // ChannelType channelType = createChannelType();
        // channelTypes.add(channelType);
        //
        // ChannelGroupType channelGroupType = createChannelGroupType();
        // channelGroupTypes.add(channelGroupType);
        //
        // ConfigDescription configDescription = createConfigDescription();
        // configDescriptions.add(configDescription);
    }

    // private ConfigDescription createConfigDescription() {
    //
    // URI uri = getURI("a");
    // List<ConfigDescriptionParameter> parameters = null;
    // List<ConfigDescriptionParameterGroup> groups = null;
    //
    // String label = "lAbel";
    // String description = "DEdescription";
    //
    // ConfigDescriptionParameter param = ConfigDescriptionParameterBuilder.create("name", Type.INTEGER)
    // .withLabel(label).withDescription(description).build();
    //
    // String name = "group";
    // String context = null;
    // Boolean advanced = false;
    // String labelx = "labelx";
    // String descriptionx = "descriptionx";
    //
    // ConfigDescriptionParameterGroup paramGroup = new ConfigDescriptionParameterGroup(name, context, advanced,
    // labelx, descriptionx);
    //
    // parameters = Arrays.asList(param);
    // groups = Arrays.asList(paramGroup);
    //
    // return new ConfigDescription(uri, parameters, groups);
    // }
    //
    // private ThingType createThingType() {
    //
    // ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, "mythingtype");
    //
    // Map<String, String> properties = new HashMap<String, String>();
    // properties.put(Thing.PROPERTY_VENDOR, "Wolf GmbH");
    // properties.put(Thing.PROPERTY_MODEL_ID, "Solar SM1 Module");
    // properties.put(Thing.PROPERTY_HARDWARE_VERSION, "3.4.1");
    // properties.put(Thing.PROPERTY_FIRMWARE_VERSION, "1.60");
    // properties.put("slaveAddress", "0x05");
    // properties.put("masterAddress", "0xFF");
    //
    // String label = "MyType";
    // String description = "My desc";
    // // List<ChannelDefinition> channelDefinitions = Arrays
    // // .asList(new ChannelDefinition("cdfID", getChannelTypeUNID("channel5")));
    // List<ChannelDefinition> channelDefinitions = null;
    // List<ChannelGroupDefinition> channelGroupDefinitions = Arrays
    // .asList(new ChannelGroupDefinition("id", getChannelGroupTypeUID("boilerGroupTypeX")));
    //
    // URI configDescriptionURI = getURI("a");
    //
    // ThingType thingType = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description,
    // channelDefinitions, channelGroupDefinitions, properties, configDescriptionURI);
    //
    // return thingType;
    // }
    //
    // @SuppressWarnings("deprecation")
    // private ChannelGroupType createChannelGroupType(IEBusCommand command) {
    //
    // ChannelDefinition cd = new ChannelDefinition("id", getChannelTypeUID("channel5"), null, "label", "description");
    //
    // List<ChannelDefinition> asList = Arrays.asList(cd);
    //
    // ChannelGroupType cgt = new ChannelGroupType(getChannelGroupTypeUID("boilerGroupTypeX"), false, "label",
    // "description", asList);
    //
    // return cgt;
    // }

    private ChannelType createChannelType(IEBusValue value, IEBusCommand command) {

        // String rid = "cid" + random.nextInt(1000);

        if (StringUtils.isNotEmpty(value.getName()) && StringUtils.isNotEmpty(command.getId())) {

            ChannelTypeUID uid = generateChannelTypeUID(command, value);
            boolean advanced = false;
            String itemType = "Number";
            ChannelKind kind = ChannelKind.STATE;
            String label = StringUtils.defaultIfEmpty(value.getLabel(), value.getName());
            String description = "My Description";
            String category = "CAT";
            Set<String> tags = null;
            StateDescription state = null;
            EventDescription event = null;
            URI configDescriptionURI = null;// getURI("a");

            return new ChannelType(uid, advanced, itemType, kind, label, description, category, tags, state, event,
                    configDescriptionURI);
        }

        return null;
    }

    protected URI getURI(String id) {
        try {
            return new URI(BINDING_ID + ":" + id);
        } catch (URISyntaxException e) {
            logger.error("error!", e);
        }
        return null;
    }

    public void initConfigDescriptions(List<EBusCommandCollection> collections) {

        List<ParameterOption> options = new ArrayList<>();

        // transform options
        for (EBusCommandCollection collection : collections) {
            options.add(new ParameterOption(collection.getId(), collection.getLabel()));
        }

        List<ConfigDescriptionParameter> parameters = new ArrayList<>();

        parameters.add(ConfigDescriptionParameterBuilder.create("device", Type.TEXT).withLabel("Devices")
                .withOptions(options).withMultiple(true).withLimitToOptions(true).build());

        parameters.add(ConfigDescriptionParameterBuilder.create("useStandardCommands", Type.BOOLEAN)
                .withLabel("Use standard commands").withDescription("Use standard eBus commands").withRequired(true)
                .withDefault("true").build());

        parameters.add(
                ConfigDescriptionParameterBuilder.create("masterAddress", Type.TEXT).withLabel("eBus Master Address")
                        .withDescription("Master address of this node as HEX").withRequired(false).build());

        parameters
                .add(ConfigDescriptionParameterBuilder.create("slaveAddress", Type.TEXT).withLabel("eBus Slave Address")
                        .withDescription("Slave address of this node as HEX").withRequired(false).build());

        ConfigDescription n = new ConfigDescription(getURI("nodeConfiguration"), parameters);

        configDescriptions.put(n.getUID(), n);
    }

    private void updateX(EBusCommandCollection collection) {

        List<ChannelGroupDefinition> channelGroupDefinitions = new ArrayList<>();

        for (IEBusCommand command : collection.getCommands()) {

            List<ChannelDefinition> channelDefinitions = new ArrayList<>();
            List<IEBusValue> list = new ArrayList<>();

            if (command.getMasterTypes() != null && !command.getMasterTypes().isEmpty()) {
                list.addAll(command.getMasterTypes());
            }
            if (command.getSlaveTypes() != null && !command.getSlaveTypes().isEmpty()) {
                list.addAll(command.getSlaveTypes());
            }

            for (IEBusValue value : list) {
                if (StringUtils.isNotEmpty(value.getName())) {

                    ChannelType channelType = createChannelType(value, command);

                    if (channelType != null) {
                        channelTypes.put(channelType.getUID(), channelType);

                        ChannelDefinition cd = new ChannelDefinition(value.getName(), channelType.getUID(), null,
                                value.getLabel(), null);

                        channelDefinitions.add(cd);
                    }

                }

            }

            if (StringUtils.isNotEmpty(command.getId())) {
                // ChannelGroupTypeUID typeUID = getChannelGroupTypeUID(command.getId())
                ChannelGroupTypeUID groupTypeUID = generateChannelGroupTypeUID(command);

                ChannelGroupType cgt = new ChannelGroupType(groupTypeUID, false, command.getId(),
                        command.getDescription(), channelDefinitions);
                channelGroupTypes.put(cgt.getUID(), cgt);

                String cgdid = "cgdid" + random.nextInt(10000);
                cgdid = command.getId().replace('.', '-') + "-" + command.getType();

                ChannelGroupDefinition groupDefinition = new ChannelGroupDefinition(cgdid, groupTypeUID,
                        command.getId(), command.getDescription());

                channelGroupDefinitions.add(groupDefinition);
            }

        }

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, collection.getId());

        String label = collection.getLabel();
        String description = collection.getAsString("description");

        // List<ChannelDefinition> channelDefinitions = Arrays
        // .asList(new ChannelDefinition("cdfID", getChannelTypeUNID("channel5")));
        // List<ChannelDefinition> channelDefinitions = null;
        // List<ChannelGroupDefinition> channelGroupDefinitions = Arrays
        // .asList(new ChannelGroupDefinition("id", getChannelGroupTypeUNID(command.getId())));

        // URI configDescriptionURI = getURI("a");

        ArrayList<ChannelDefinition> channelDefinitions2 = new ArrayList<>();

        ThingType thingType = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description,
                channelDefinitions2, channelGroupDefinitions, null, getURI("nodeConfiguration"));

        thingTypes.put(thingType.getUID(), thingType);

    }

    @Override
    public void update(List<EBusCommandCollection> collections) {

        random = new Random();
        initConfigDescriptions(collections);

        for (EBusCommandCollection collection : collections) {
            updateX(collection);
        }
        //
        // // ChannelDefinition cd = new ChannelDefinition("id", getChannelTypeUNID("channel5"), null, "label",
        // // "description");
        //
        // List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        // List<ChannelGroupDefinition> channelGroupDefinitions = new ArrayList<>();
        //

        //
        // // int nextInt = r.nextInt(1000);
        //
        // ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, "autotype1");
        //
        // String label = "AutoType1";
        // String description = "My desc";
        // // List<ChannelDefinition> channelDefinitions = Arrays
        // // .asList(new ChannelDefinition("cdfID", getChannelTypeUNID("channel5")));
        // // List<ChannelDefinition> channelDefinitions = null;
        // // List<ChannelGroupDefinition> channelGroupDefinitions = Arrays
        // // .asList(new ChannelGroupDefinition("id", getChannelGroupTypeUNID(command.getId())));
        //
        // // URI configDescriptionURI = getURI("a");
        //
        // ArrayList channelDefinitions2 = new ArrayList<>();
        //
        // ThingType thingType = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description,
        // channelDefinitions2, channelGroupDefinitions, null, getURI("nodeConfiguration"));
        //
        // thingTypes.put(thingType.getUID(), thingType);
        //
        // // label = "eBus Node";
        // // description = "eBus Node";
        // // thingTypeUID = new ThingTypeUID(BINDING_ID, "node");
        // // thingType = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description, null, null, null,
        // null);
        // //
        // // thingTypes.add(thingType);

    }

    @Override
    public void clear() {
        channelGroupTypes.clear();
        channelTypes.clear();
        configDescriptions.clear();
        thingTypes.clear();
    }

}
