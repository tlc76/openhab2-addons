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

import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusNestedValue;
import de.csdev.ebus.command.IEBusValue;
import de.csdev.ebus.command.datatypes.ext.EBusTypeBytes;
import de.csdev.ebus.command.datatypes.ext.EBusTypeDateTime;
import de.csdev.ebus.command.datatypes.ext.EBusTypeString;
import de.csdev.ebus.command.datatypes.std.EBusTypeBit;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusTypeProviderImpl extends EBusTypeProviderBase implements EBusTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(EBusTypeProviderImpl.class);

    public void activate() {
        logger.info("Loading eBUS Type Provider ...");
    }

    public void dispose() {
        logger.info("Stopping eBUS Type Provider ...");
        clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.ebus.thing.EBusGenerator#clear()
     */
    @Override
    public void clear() {
        channelGroupTypes.clear();
        channelTypes.clear();
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

            logger.trace("Add channel {} for method {}", channelType.getUID(), mainMethod.getMethod());

            // add to global list
            channelTypes.put(channelType.getUID(), channelType);

            // store command id and value name
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(EBusBindingConstants.COLLECTION, mainMethod.getParent().getParentCollection().getId());
            properties.put(EBusBindingConstants.COMMAND, mainMethod.getParent().getId());
            properties.put(EBusBindingConstants.VALUE_NAME, value.getName());

            return new ChannelDefinition(EBusBindingUtils.formatId(value.getName()), channelType.getUID(), properties,
                    value.getLabel(), null);
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
        if (StringUtils.isNotEmpty(value.getName()) && StringUtils.isNotEmpty(mainMethod.getParent().getId())) {

            ChannelTypeUID uid = EBusBindingUtils.generateChannelTypeUID(value);

            IEBusCommandMethod commandSetter = mainMethod.getParent().getCommandMethod(IEBusCommandMethod.Method.SET);

            boolean readOnly = commandSetter == null;
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

            if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeBit.TYPE_BIT)) {
                itemType = "Switch";

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeDateTime.TYPE_DATETIME)) {
                itemType = "DateTime";

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeString.TYPE_STRING)) {
                itemType = "String";

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeBytes.BYTES)) {
                itemType = "String";

            } else if (options != null) {
                itemType = "Number";

            }

            boolean advanced = value.getName().startsWith("_");
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
    private ThingType createThingType(IEBusCommandCollection collection,
            ArrayList<ChannelDefinition> channelDefinitions, List<ChannelGroupDefinition> channelGroupDefinitions) {

        ThingTypeUID thingTypeUID = EBusBindingUtils.generateThingTypeUID(collection);

        String label = collection.getLabel();
        String description = collection.getDescription();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(EBusBindingConstants.COLLECTION, collection.getId());
        properties.put("collectionHash", String.valueOf(collection.hashCode()));

        return new ThingType(thingTypeUID, supportedBridgeTypeUIDs, label, description, channelDefinitions,
                channelGroupDefinitions, properties, EBusBindingConstants.CONFIG_DESCRIPTION_URI_NODE);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.ebus.thing.EBusGenerator#update(java.util.List)
     */
    @Override
    public void update(List<IEBusCommandCollection> collections) {

        for (IEBusCommandCollection collection : collections) {
            updateCollection(collection);
        }
        logger.info("Generated all eBUS command collections ...");
    }

    /**
     * @param collection
     */
    private void updateCollection(IEBusCommandCollection collection) {

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
                            logger.trace("Add channel definition {}", value.getName());
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
