/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus;

import java.net.URI;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.ebus.internal.EBusBindingUtils;

/**
 * The {@link EBusBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBindingConstants {

    public static final String BINDING_ID = "ebus";
    public static final String BINDING_PID = "org.openhab.ebus";

    // bridge
    public static final ThingTypeUID THING_TYPE_EBUS_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // ebus thing properties
    public static final String MASTER_ADDRESS = "masterAddress";
    public static final String SLAVE_ADDRESS = "slaveAddress";
    public static final String POLLING = "polling";

    // properties for ebus connection
    public static final String SERIAL_PORT = "serialPort";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String PORT = "port";
    public static final String ADVANCED_LOGGING = "advancedLogging";

    public static final String CONFIGURATION_URL = "configurationUrl";
    public static final String CONFIGURATION_URL1 = "configurationUrl1";
    public static final String CONFIGURATION_URL2 = "configurationUrl2";
    public static final String CONFIGURATION_BUNDLE_URL = "configurationBundleUrl";

    // properties to map ebus core configurations
    // public static final String COLLECTION = "collection";
    public static final String COMMAND = "command";
    public static final String METHOD = "method";
    public static final String VALUE_NAME = "valueName";

    public static final String FILTER_ACCEPT_MASTER = "filterAcceptMaster";
    public static final String FILTER_ACCEPT_SLAVE = "filterAcceptSlave";
    public static final String FILTER_ACCEPT_BROADCAST = "filterAcceptBroadcasts";

    // channel group id
    public static final String METRICS = "metrics";

    // channel ids
    public static final String RECEIVED_TELEGRAMS = "receivedTelegrams";
    public static final String FAILED_TELEGRAMS = "failedTelegrams";
    public static final String RESOLVED_TELEGRAMS = "resolvedTelegrams";
    public static final String UNRESOLVED_TELEGRAMS = "unresolvedTelegrams";
    public static final String FAILED_RATIO = "failedRatio";
    public static final String UNRESOLVED_RATIO = "unresolvedRatio";

    // configuration uris
    public static final URI CONFIG_DESCRIPTION_URI_NODE = EBusBindingUtils
            .getURI("thing-type:" + BINDING_ID + ":nodeConfig");
    public static final URI CONFIG_DESCRIPTION_URI_POLLING_CHANNEL = EBusBindingUtils
            .getURI("channel-type:" + BINDING_ID + ":pollingChannel");
    public static final URI CONFIG_DESCRIPTION_URI_NULL_CHANNEL = EBusBindingUtils
            .getURI("channel-type:" + BINDING_ID + ":nullChannel");
}
