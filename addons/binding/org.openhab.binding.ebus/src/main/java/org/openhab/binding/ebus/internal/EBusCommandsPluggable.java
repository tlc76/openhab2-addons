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
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.openhab.binding.ebus.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.handler.EBusHandler;
import org.openhab.binding.ebus.thing.EBusTypeProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.core.connection.EBusEmulatorConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
import de.csdev.ebus.service.device.EBusDeviceTable;
import de.csdev.ebus.utils.EBusConsoleUtils;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusCommandsPluggable implements ConsoleCommandExtension {

    @SuppressWarnings("unused")
    private final Logger logger = LoggerFactory.getLogger(EBusCommandsPluggable.class);

    private static final String CMD = "ebus";

    private static final String SUBCMD_LIST = "list";

    private static final String SUBCMD_SEND = "send";

    private static final String SUBCMD_DEVICES = "devices";

    private static final String SUBCMD_RESOLVE = "resolve";

    private ThingRegistry thingRegistry;

    private EBusTypeProvider typeProvider;

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
    protected void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public void setTypeProvider(EBusTypeProvider typeProvider) {
        this.typeProvider = typeProvider;
    }

    public void unsetTypeProvider(EBusTypeProvider typeProvider) {
        this.typeProvider = null;
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

                console.print(EBusConsoleUtils.getDeviceTableInformation(collections, deviceTable));
            }

        } else {

            EBusClient client = bridge.getLibClient().getClient();
            EBusDeviceTable deviceTable = client.getDeviceTable();
            Collection<IEBusCommandCollection> collections = client.getCommandCollections();

            console.print(EBusConsoleUtils.getDeviceTableInformation(collections, deviceTable));

        }
    }

    private void resolve(byte[] data, Console console) {
        console.println(EBusConsoleUtils.analyzeTelegram(typeProvider.getCommandRegistry(), data));
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

        } else if (StringUtils.equals(args[0], SUBCMD_DEVICES)) {

            if (args.length == 2) {
                devices(args, console, getBridge(args[1], console));
            } else {
                devices(args, console, null);
            }

        } else if (StringUtils.equals(args[0], SUBCMD_RESOLVE)) {
            resolve(EBusUtils.toByteArray(args[1]), console);

        } else if (StringUtils.equals(args[0], SUBCMD_SEND)) {
            EBusBridgeHandler bridge = null;

            if (args.length == 3) {
                bridge = getBridge(args[2], console);
            } else {
                bridge = getFirstBridge(console);
            }

            if (bridge != null) {
                byte[] data = EBusUtils.toByteArray(args[1]);
                bridge.getLibClient().sendTelegram(data);
            }
        }
    }

    private EBusBridgeHandler getFirstBridge(Console console) {
        Collection<EBusBridgeHandler> bridgeHandlers = getAllEBusBridgeHandlers();
        if (!bridgeHandlers.isEmpty()) {
            return bridgeHandlers.iterator().next();
        }

        console.println("Error: Unable to find an eBUS bridge");
        return null;
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

        List<String> list = new ArrayList<>();
        list.add(String.format(line, CMD, SUBCMD_LIST, "lists all eBUS devices"));
        list.add(String.format(line2, CMD, SUBCMD_SEND, "\"<ebus telegram>\" [<bridgeUID>]",
                "sends a raw hex telegram to an eBUS bridge or if not set to first bridge"));
        list.add(String.format(line2, CMD, SUBCMD_DEVICES, "[<bridgeUID>]",
                "lists all devices connect to an eBUS bridge or list only a specific bridge"));
        list.add(String.format(line2, CMD, SUBCMD_RESOLVE, "\"<ebus telegram>\"", "resolves and analyze a telegram"));

        return list;
    }

}