/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus;

import java.net.URI;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.ebus.internal.EbusBindingUtils;

/**
 * The {@link EBusBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBindingConstants {

    public static final String BINDING_ID = "ebus";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_EBUS_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

    // List of configuration ids
    public static final String CONFIG_DEVICE = "device";
    public static final String CONFIG_USE_STANDARD_COMMANDS = "useStandardCommands";
    public static final String CONFIG_MASTER_ADDRESS = "masterAddress";
    public static final String CONFIG_SLAVE_ADDRESS = "slaveAddress";
    public static final String CONFIG_POLLING = "polling";

    public static final String PROPERTY_COMMAND = "command";

    // configuration uris
    public static final URI CONFIG_DESCRIPTION_URI_NODE = EbusBindingUtils.getURI(BINDING_ID + ":nodeConfiguration");
    public static final URI CONFIG_DESCRIPTION_URI_POLLING_CHANNEL = EbusBindingUtils
            .getURI(BINDING_ID + ":pollingChannel");
}
