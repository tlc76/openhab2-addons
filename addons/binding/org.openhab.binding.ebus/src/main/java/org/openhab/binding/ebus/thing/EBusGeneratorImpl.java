package org.openhab.binding.ebus.thing;

import static org.openhab.binding.ebus.EBusBindingConstants.BINDING_ID;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.core.library.CoreItemFactory;
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
import org.eclipse.smarthome.core.types.StateOption;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.EBusCommandCollection;
import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusCommandChannel;
import de.csdev.ebus.command.IEBusValue;

public class EBusGeneratorImpl extends EBusGeneratorBase implements EBusGenerator {

    private final Logger logger = LoggerFactory.getLogger(EBusGeneratorImpl.class);

    private Random random;

    public EBusGeneratorImpl() {

    }

    private ChannelType createChannelType(IEBusValue value, IEBusCommandChannel mainChannel) {

        if (StringUtils.isNotEmpty(value.getName()) && StringUtils.isNotEmpty(mainChannel.getParent().getId())
                && !value.getName().startsWith("_")) {

            ChannelTypeUID uid = generateChannelTypeUID(mainChannel.getParent(), value);

            IEBusCommandChannel commandChannelSet = mainChannel.getParent().getCommandChannel(IEBusCommand.Type.SET);
            Map<String, Object> valueProperties = value.getProperties();
            boolean readOnly = commandChannelSet == null;

            List<StateOption> options = null;
            if (value.getMapping() != null && !value.getMapping().isEmpty()) {
                options = new ArrayList<StateOption>();
                for (Entry<String, String> mapping : value.getMapping().entrySet()) {
                    options.add(new StateOption(mapping.getKey(), mapping.getValue()));
                }
            }

            boolean advanced = false;
            String itemType = options != null ? CoreItemFactory.STRING : CoreItemFactory.NUMBER;
            ChannelKind kind = ChannelKind.STATE;
            String label = StringUtils.defaultIfEmpty(value.getLabel(), value.getName());
            String description = "My Description";
            String category = null;
            Set<String> tags = null;
            StateDescription state = null;
            EventDescription event = null;
            URI configDescriptionURI = null;

            String pattern = value.getFormat();

            state = new StateDescription(value.getMax(), value.getMin(), value.getStep(), pattern, readOnly, options);

            return new ChannelType(uid, advanced, itemType, kind, label, description, category, tags, state, event,
                    configDescriptionURI);
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

        parameters.add(ConfigDescriptionParameterBuilder.create(EBusBindingConstants.CONFIG_DEVICE, Type.TEXT)
                .withLabel("Devices").withOptions(options).withMultiple(true).withLimitToOptions(true).build());

        parameters.add(ConfigDescriptionParameterBuilder
                .create(EBusBindingConstants.CONFIG_USE_STANDARD_COMMANDS, Type.BOOLEAN)
                .withLabel("Use standard commands").withDescription("Use standard eBus commands").withRequired(true)
                .withDefault("true").build());

        parameters.add(ConfigDescriptionParameterBuilder.create(EBusBindingConstants.CONFIG_MASTER_ADDRESS, Type.TEXT)
                .withLabel("eBus Master Address").withDescription("Master address of this node as HEX")
                .withRequired(false).build());

        parameters.add(ConfigDescriptionParameterBuilder.create(EBusBindingConstants.CONFIG_SLAVE_ADDRESS, Type.TEXT)
                .withLabel("eBus Slave Address").withDescription("Slave address of this node as HEX")
                .withRequired(false).build());

        ConfigDescription n = new ConfigDescription(EBusBindingConstants.CONFIG_DESCRIPTION_URI_THING, parameters);

        configDescriptions.put(n.getUID(), n);
    }

    private void updateX(EBusCommandCollection collection) {

        List<ChannelGroupDefinition> channelGroupDefinitions = new ArrayList<>();

        for (IEBusCommand command : collection.getCommands()) {

            List<ChannelDefinition> channelDefinitions = new ArrayList<>();
            List<IEBusValue> list = new ArrayList<>();

            Collection<IEBusCommand.Type> commandChannelTypes = command.getCommandChannelTypes();

            IEBusCommandChannel mainChannel = null;
            if (commandChannelTypes.contains(IEBusCommand.Type.GET)) {
                mainChannel = command.getCommandChannel(IEBusCommand.Type.GET);

            } else if (commandChannelTypes.contains(IEBusCommand.Type.BROADCAST)) {
                mainChannel = command.getCommandChannel(IEBusCommand.Type.BROADCAST);

            } else {
                logger.warn("EBus Command {} only contains a setter channel!", command.getId());
                // mh ... not correct!
            }

            if (mainChannel != null) {

                if (mainChannel.getMasterTypes() != null && !mainChannel.getMasterTypes().isEmpty()) {
                    list.addAll(mainChannel.getMasterTypes());
                }
                if (mainChannel.getSlaveTypes() != null && !mainChannel.getSlaveTypes().isEmpty()) {
                    list.addAll(mainChannel.getSlaveTypes());
                }

                for (IEBusValue value : list) {
                    if (StringUtils.isNotEmpty(value.getName())) {

                        ChannelType channelType = createChannelType(value, mainChannel);

                        if (channelType != null) {
                            logger.info("Add channel {} for type {}", channelType.getUID(), mainChannel.getType());
                            channelTypes.put(channelType.getUID(), channelType);

                            ChannelDefinition cd = new ChannelDefinition(value.getName(), channelType.getUID(), null,
                                    value.getLabel(), null);

                            logger.info("Add channel definition {}", value.getName());
                            channelDefinitions.add(cd);
                        }

                    }

                }
            }
            if (StringUtils.isNotEmpty(command.getId())) {
                // ChannelGroupTypeUID typeUID = getChannelGroupTypeUID(command.getId())
                ChannelGroupTypeUID groupTypeUID = generateChannelGroupTypeUID(command);

                @SuppressWarnings("deprecation")
                ChannelGroupType cgt = new ChannelGroupType(groupTypeUID, false, command.getId(),
                        command.getDescription(), channelDefinitions);

                channelGroupTypes.put(cgt.getUID(), cgt);

                String cgdid = "cgdid" + random.nextInt(10000);
                cgdid = command.getId().replace('.', '-');

                ChannelGroupDefinition groupDefinition = new ChannelGroupDefinition(cgdid, groupTypeUID,
                        command.getId(), command.getDescription());

                channelGroupDefinitions.add(groupDefinition);
            }

        }

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, collection.getId());

        String label = collection.getLabel();
        String description = collection.getAsString("description");

        ArrayList<ChannelDefinition> channelDefinitions2 = new ArrayList<>();

        ThingType thingType = new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description,
                channelDefinitions2, channelGroupDefinitions, null, EBusBindingConstants.CONFIG_DESCRIPTION_URI_THING);

        thingTypes.put(thingType.getUID(), thingType);
    }

    @Override
    public void update(List<EBusCommandCollection> collections) {

        random = new Random();
        initConfigDescriptions(collections);

        for (EBusCommandCollection collection : collections) {
            updateX(collection);
        }

    }

    @Override
    public void clear() {
        channelGroupTypes.clear();
        channelTypes.clear();
        configDescriptions.clear();
        thingTypes.clear();
    }

}
