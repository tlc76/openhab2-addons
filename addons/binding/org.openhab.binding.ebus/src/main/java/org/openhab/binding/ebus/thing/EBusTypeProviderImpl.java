/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.thing;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
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
import org.openhab.binding.ebus.internal.EBusBindingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.cfg.datatypes.EBusTypeBit;
import de.csdev.ebus.cfg.datatypes.ext.EBusTypeBytes;
import de.csdev.ebus.cfg.datatypes.ext.EBusTypeDateTime;
import de.csdev.ebus.cfg.datatypes.ext.EBusTypeString;
import de.csdev.ebus.command.EBusCommandCollection;
import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusNestedValue;
import de.csdev.ebus.command.IEBusValue;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusTypeProviderImpl extends EBusTypeProviderBase implements EBusTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(EBusTypeProviderImpl.class);

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.ebus.thing.EBusGenerator#clear()
     */
    @Override
    public void clear() {
        channelGroupTypes.clear();
        channelTypes.clear();
        configDescriptions.clear();
        thingTypes.clear();
    }

    /**
     * @param command
     * @param mainChannel
     * @param value
     * @return
     */
    private ChannelDefinition createChannelDefinition(IEBusCommandMethod mainMethod, IEBusValue value) {

        ChannelType channelType = createChannelType(value, mainMethod);

        if (channelType != null) {

            logger.info("Add channel {} for method {}", channelType.getUID(), mainMethod.getMethod());

            // add to global list
            channelTypes.put(channelType.getUID(), channelType);

            // store command id
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(EBusBindingConstants.COMMAND, mainMethod.getParent().getId());

            return new ChannelDefinition(value.getName(), channelType.getUID(), properties, value.getLabel(), null);
        }

        return null;
    }

    /**
     * @param command
     * @param channelDefinitions
     * @return
     */
    private ChannelGroupDefinition createChannelGroupDefinition(IEBusCommand command,
            List<ChannelDefinition> channelDefinitions) {

        ChannelGroupTypeUID groupTypeUID = EBusBindingUtils.generateChannelGroupTypeUID(command);

        @SuppressWarnings("deprecation")
        ChannelGroupType cgt = new ChannelGroupType(groupTypeUID, false, command.getLabel(), command.getId(),
                channelDefinitions);

        // add to global list
        channelGroupTypes.put(cgt.getUID(), cgt);

        String cgdid = EBusBindingUtils.generateChannelGroupID(command);

        return new ChannelGroupDefinition(cgdid, groupTypeUID, command.getLabel(), command.getId());
    }

    /**
     * @param value
     * @param mainChannel
     * @return
     */
    private ChannelType createChannelType(IEBusValue value, IEBusCommandMethod mainMethod) {

        // only process valid entries
        if (StringUtils.isNotEmpty(value.getName()) && StringUtils.isNotEmpty(mainMethod.getParent().getId())
                && !value.getName().startsWith("_")) {

            ChannelTypeUID uid = EBusBindingUtils.generateChannelTypeUID(mainMethod.getParent(), value);

            IEBusCommandMethod commandChannelSet = mainMethod.getParent()
                    .getCommandMethod(IEBusCommandMethod.Method.SET);

            boolean readOnly = commandChannelSet == null;
            boolean polling = mainMethod.getType().equals(IEBusCommandMethod.Type.MASTER_SLAVE);

            // create a option list if mapping is used
            List<StateOption> options = null;
            if (value.getMapping() != null && !value.getMapping().isEmpty()) {
                options = new ArrayList<StateOption>();
                for (Entry<String, String> mapping : value.getMapping().entrySet()) {
                    options.add(new StateOption(mapping.getKey(), mapping.getValue()));
                }
            }

            // default
            String itemType = "Number";

            if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeBit.BIT)) {
                itemType = "Switch";

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeDateTime.DATETIME)) {
                itemType = "DateTime";

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeString.STRING)) {
                itemType = "Text";

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeBytes.BYTES)) {
                itemType = "Text";

            } else if (options != null) {
                itemType = "Number";

            }

            boolean advanced = false;
            ChannelKind kind = ChannelKind.STATE;
            String label = StringUtils.defaultIfEmpty(value.getLabel(), value.getName());
            String pattern = value.getFormat();
            StateDescription state = new StateDescription(value.getMax(), value.getMin(), value.getStep(), pattern,
                    readOnly, options);
            URI configDescriptionURI = polling ? EBusBindingConstants.CONFIG_DESCRIPTION_URI_POLLING_CHANNEL : null;

            String description = null;
            Set<String> tags = null;
            String category = null;
            EventDescription event = null;

            state = new StateDescription(value.getMax(), value.getMin(), value.getStep(), pattern, readOnly, options);

            return new ChannelType(uid, advanced, itemType, kind, label, description, category, tags, state, event,
                    configDescriptionURI);
        }

        return null;
    }

    /**
     * @param collection
     * @param channelDefinitions
     * @param channelGroupDefinitions
     * @return
     */
    private ThingType createThingType(EBusCommandCollection collection, ArrayList<ChannelDefinition> channelDefinitions,
            List<ChannelGroupDefinition> channelGroupDefinitions) {

        ThingTypeUID thingTypeUID = EBusBindingUtils.generateThingTypeUID(collection);

        String label = collection.getLabel();
        String description = collection.getDescription();

        return new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description, channelDefinitions,
                channelGroupDefinitions, null, EBusBindingConstants.CONFIG_DESCRIPTION_URI_NODE);
    }

    /**
     * Should be done in an xml file
     *
     * @param collections
     */
    @Deprecated
    public void initConfigDescriptions(List<EBusCommandCollection> collections) {

        List<ConfigDescriptionParameter> parameters = new ArrayList<>();

        parameters.add(ConfigDescriptionParameterBuilder.create(EBusBindingConstants.MASTER_ADDRESS, Type.TEXT)
                .withLabel("eBUS Master Address").withDescription("Master address of this node as HEX value")
                .withRequired(false).build());

        parameters.add(ConfigDescriptionParameterBuilder.create(EBusBindingConstants.SLAVE_ADDRESS, Type.TEXT)
                .withLabel("eBUS Slave Address").withDescription("Slave address of this node as HEX value")
                .withRequired(false).build());

        ConfigDescription configDescription = new ConfigDescription(EBusBindingConstants.CONFIG_DESCRIPTION_URI_NODE,
                parameters);

        // add to global list
        configDescriptions.put(EBusBindingConstants.CONFIG_DESCRIPTION_URI_NODE, configDescription);

        // channel config

        parameters = new ArrayList<>();
        parameters.add(ConfigDescriptionParameterBuilder.create(EBusBindingConstants.POLLING, Type.DECIMAL)
                .withUnit("s").withLabel("Polling")
                .withDescription(
                        "Set to poll this channel every n seconds. <br />All channels that are part of this group will also be polled!")
                .withUnitLabel("Seconds").build());

        configDescription = new ConfigDescription(EBusBindingConstants.CONFIG_DESCRIPTION_URI_POLLING_CHANNEL,
                parameters);

        // add to global list
        configDescriptions.put(EBusBindingConstants.CONFIG_DESCRIPTION_URI_POLLING_CHANNEL, configDescription);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.ebus.thing.EBusGenerator#update(java.util.List)
     */
    @Override
    public void update(List<EBusCommandCollection> collections) {

        initConfigDescriptions(collections);

        for (EBusCommandCollection collection : collections) {
            updateCollection(collection);
        }

    }

    /**
     * @param collection
     */
    private void updateCollection(EBusCommandCollection collection) {

        List<ChannelGroupDefinition> channelGroupDefinitions = new ArrayList<>();

        for (IEBusCommand command : collection.getCommands()) {

            List<ChannelDefinition> channelDefinitions = new ArrayList<>();
            List<IEBusValue> list = new ArrayList<>();

            Collection<IEBusCommandMethod.Method> commandChannelTypes = command.getCommandChannelMethods();

            IEBusCommandMethod mainMethod = null;
            if (commandChannelTypes.contains(IEBusCommandMethod.Method.GET)) {
                mainMethod = command.getCommandMethod(IEBusCommandMethod.Method.GET);

            } else if (commandChannelTypes.contains(IEBusCommandMethod.Method.BROADCAST)) {
                mainMethod = command.getCommandMethod(IEBusCommandMethod.Method.BROADCAST);

            } else {
                logger.warn("eBUS command {} only contains a setter channel!", command.getId());
                // mh ... not correct!
            }

            if (mainMethod != null) {

                if (mainMethod.getMasterTypes() != null && !mainMethod.getMasterTypes().isEmpty()) {
                    list.addAll(mainMethod.getMasterTypes());
                }

                if (mainMethod.getSlaveTypes() != null && !mainMethod.getSlaveTypes().isEmpty()) {
                    list.addAll(mainMethod.getSlaveTypes());
                }

                // now check for nested values
                List<IEBusValue> childList = new ArrayList<IEBusValue>();
                for (IEBusValue value : list) {
                    if (value instanceof IEBusNestedValue) {
                        childList.addAll(((IEBusNestedValue) value).getChildren());
                    }
                }
                list.addAll(childList);

                // *****************************************
                // generate a channel for each ebus value
                // *****************************************

                for (IEBusValue value : list) {
                    if (StringUtils.isNotEmpty(value.getName())) {

                        ChannelDefinition channelDefinition = createChannelDefinition(mainMethod, value);
                        if (channelDefinition != null) {
                            logger.info("Add channel definition {}", value.getName());
                            channelDefinitions.add(channelDefinition);
                        }
                    }
                }
            }

            // *****************************************
            // create a channel group for each command
            // *****************************************

            if (StringUtils.isNotEmpty(command.getId())) {
                ChannelGroupDefinition channelGroupDefinition = createChannelGroupDefinition(command,
                        channelDefinitions);
                channelGroupDefinitions.add(channelGroupDefinition);
            }

        }

        // *****************************************
        // generate a thing for this collection
        // *****************************************

        ThingType thingType = createThingType(collection, null, channelGroupDefinitions);
        thingTypes.put(thingType.getUID(), thingType);
    }

}
