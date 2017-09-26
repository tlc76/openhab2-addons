/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.EBusBindingConstants.BINDING_ID;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusValue;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBindingUtils {

    private static final Logger logger = LoggerFactory.getLogger(EBusBindingConstants.class);

    public static URI getURI(String id) {
        try {
            return new URI(id);
        } catch (URISyntaxException e) {
            logger.error("error!", e);
        }
        return null;
    }

    public static ChannelTypeUID generateChannelTypeUID(IEBusValue value) {
        String id = generateValueId(value);
        return new ChannelTypeUID(BINDING_ID, id);
    }

    public static ChannelUID generateChannelUID(IEBusValue value, ThingUID thingUID) {
        IEBusCommandMethod method = value.getParent();
        IEBusCommand command = method.getParent();
        return new ChannelUID(thingUID, generateChannelGroupID(command), formatId(value.getName()));
    }

    public static ChannelUID generateChannelUID(IEBusCommand command, String valueName, ThingUID thingUID) {
        return new ChannelUID(thingUID, generateChannelGroupID(command), formatId(valueName));
    }

    public static ChannelGroupTypeUID generateChannelGroupTypeUID(IEBusCommand command) {
        return new ChannelGroupTypeUID(BINDING_ID, generateChannelGroupID(command));
    }

    public static ThingTypeUID generateThingTypeUID(IEBusCommandCollection collection) {
        return new ThingTypeUID(BINDING_ID, collection.getId());
    }

    public static String generateChannelGroupID(IEBusCommand command) {
        IEBusCommandCollection parentCollection = command.getParentCollection();
        return String.format("%s_%s", parentCollection.getId(), formatId(command.getId()));
    }

    public static String generateValueId(IEBusValue value) {
        IEBusCommandMethod method = value.getParent();
        IEBusCommand command = method.getParent();
        return String.format("%s_%s", generateChannelGroupID(command), formatId(value.getName()));
    }

    public static String formatId(String id) {
        return id.replace('_', '-').replace('.', '_');
    }
}
