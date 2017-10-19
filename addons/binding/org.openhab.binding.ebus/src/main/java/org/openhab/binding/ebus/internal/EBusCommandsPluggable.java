/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.handler.EBusHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandUtils;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.connection.EBusEmulatorConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
import de.csdev.ebus.service.device.EBusDeviceTable;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusCommandsPluggable implements ConsoleCommandExtension {

    private final Logger logger = LoggerFactory.getLogger(EBusCommandsPluggable.class);

    private static final String CMD = "ebus";

    private static final String SUBCMD_LIST = "list";

    private static final String SUBCMD_SEND = "send";

    private static final String SUBCMD_DEVICES = "devices";

    private static final String SUBCMD_RESOLVE = "resolve";

    @SuppressWarnings("unused")
    private ManagedThingProvider managedThingProvider;

    private ThingRegistry thingRegistry;

    /**
     * Activating this component - called from DS.
     *
     * @param componentContext
     */
    protected void activate(ComponentContext componentContext) {
        // noop
    }

    /**
     * Deactivating this component - called from DS.
     */
    protected void deactivate(ComponentContext componentContext) {
        // noop
    }

    @Reference
    protected void setManagedThingProvider(ManagedThingProvider managedThingProvider) {
        this.managedThingProvider = managedThingProvider;
    }

    @Reference
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    protected void unsetManagedThingProvider(ManagedThingProvider managedThingProvider) {
        this.managedThingProvider = null;
    }

    protected void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    @Override
    public String getCommand() {
        return CMD;
    }

    @Override
    public String getDescription() {
        return "eBUS commands";
    }

    private Collection<EBusBridgeHandler> getAllEBusBridgeHandlers() {
        Collection<EBusBridgeHandler> result = new ArrayList<EBusBridgeHandler>();
        for (Thing thing : thingRegistry.getAll()) {

            if (thing.getHandler() instanceof EBusBridgeHandler) {
                result.add((EBusBridgeHandler) thing.getHandler());
            }
        }

        return result;
    }

    private void list(String[] args, Console console) {
        console.println(String.format("%-40s | %-40s | %-10s", "Thing UID", "Label", "Type"));
        console.println(String.format("%-40s-+-%-40s-+-%-10s", StringUtils.repeat("-", 40), StringUtils.repeat("-", 40),
                StringUtils.repeat("-", 10)));

        for (Thing thing : thingRegistry.getAll()) {

            if (thing.getHandler() instanceof EBusBridgeHandler || thing.getHandler() instanceof EBusHandler) {

                String type = "node";

                if (thing.getHandler() instanceof EBusBridgeHandler) {
                    EBusBridgeHandler bridge = (EBusBridgeHandler) thing.getHandler();
                    @SuppressWarnings("null")
                    IEBusConnection connection = bridge.getLibClient().getClient().getController().getConnection();

                    if (connection instanceof EBusEmulatorConnection) {
                        type = "bridge (emulator)";
                    } else {
                        type = "bridge";
                    }
                }

                console.println(String.format("%-40s | %-40s | %-10s", thing.getUID(), thing.getLabel(), type));
            }
        }
    }

    private void devices(String[] args, Console console, EBusBridgeHandler bridge) {

        if (bridge == null) {

            for (EBusBridgeHandler handler : getAllEBusBridgeHandlers()) {

                EBusClient client = handler.getLibClient().getClient();
                EBusDeviceTable deviceTable = client.getDeviceTable();
                Collection<IEBusCommandCollection> collections = client.getCommandCollections();

                console.print(deviceTable.getDeviceTableInformation(collections));
            }

        } else {

            EBusClient client = bridge.getLibClient().getClient();
            EBusDeviceTable deviceTable = client.getDeviceTable();
            Collection<IEBusCommandCollection> collections = client.getCommandCollections();

            console.print(deviceTable.getDeviceTableInformation(collections));

        }
    }

    private void resolve(byte[] data, Console console, EBusBridgeHandler bridge) {

        List<IEBusCommandMethod> methods = bridge.getLibClient().getClient().getConfigurationProvider().find(data);

        console.println(String.format("Found %s command methods for this telegram.", methods.size()));

        for (IEBusCommandMethod method : methods) {
            try {
                Map<String, Object> result = EBusCommandUtils.decodeTelegram(method, data);

                console.println(String.format("Decode command %s with method %s.", method.getParent().getId(),
                        method.getMethod()));
                for (Entry<String, Object> entry : result.entrySet()) {
                    console.println(String.format("  %-20s = %s", entry.getKey(), entry.getValue()));
                }

            } catch (EBusTypeException e) {
                logger.error("error!", e);
            }
        }
    }

    @Override
    public void execute(String[] args, Console console) {

        if (args.length == 0) {
            list(args, console);
            return;
        }

        if (StringUtils.equals(args[0], SUBCMD_LIST)) {
            list(args, console);
            return;
        }

        if (StringUtils.equals(args[0], SUBCMD_DEVICES)) {

            if (args.length == 2) {
                devices(args, console, getBridge(args[1], console));
            } else {
                devices(args, console, null);
            }

            return;
        }

        final EBusBridgeHandler bridge = getBridge(args[1], console);
        if (bridge == null) {
            return;
        }

        byte[] data = EBusUtils.toByteArray(args[2]);

        if (StringUtils.equals(args[0], SUBCMD_SEND)) {
            bridge.getLibClient().sendTelegram(data);

        } else if (StringUtils.equals(args[0], SUBCMD_RESOLVE)) {
            resolve(data, console, bridge);

        }

    }

    private EBusBridgeHandler getBridge(String bridgeUID, Console console) {

        Thing thing;
        try {
            thing = thingRegistry.get(new ThingUID(bridgeUID));
        } catch (IllegalArgumentException e) {
            console.println("Error: " + e.getMessage());
            return null;
        }

        if (thing == null) {
            console.println(String.format("Error: Unable to find a thing with thingUID %s", bridgeUID));
            return null;
        }

        if (!(thing.getHandler() instanceof EBusBridgeHandler)) {
            console.println(String.format("Error: Given thingUID %s is not an eBUS bridge!", bridgeUID));
            return null;
        }

        return (EBusBridgeHandler) thing.getHandler();

    }

    @Override
    public List<String> getUsages() {

        String line = "%s %s - %s";
        String line2 = "%s %s %s - %s";
        return Arrays.asList(String.format(line, CMD, SUBCMD_LIST, "lists all eBUS devices"), String.format(line2, CMD,
                SUBCMD_SEND, "send <thingUID> \"<ebus telegram>\"", "sends a raw hex telegram to an eBUS bridge"));
    }

}