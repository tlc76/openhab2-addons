/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import java.util.Arrays;
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

import de.csdev.ebus.command.EBusCommandUtils;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.connection.EBusEmulatorConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
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

    @SuppressWarnings("unused")
    private ManagedThingProvider managedThingProvider;

    private ThingRegistry thingRegistry;

    /**
     * Activating this component - called from DS.
     *
     * @param componentContext
     */
    protected void activate(ComponentContext componentContext) {
        // super.initialize(componentContext.getBundleContext());
        // logger.error("************************+ Command started!");
    }

    /**
     * Deactivating this component - called from DS.
     */
    protected void deactivate(ComponentContext componentContext) {
        // super.dispose();
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
        return "eBUS";
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

    @Override
    public void execute(String[] args, Console console) {

        if (StringUtils.equals(args[0], SUBCMD_LIST)) {
            list(args, console);
            return;
        }

        final EBusBridgeHandler bridge = getBridge(args[1], console);
        if (bridge == null) {
            return;
        }

        byte[] data = EBusUtils.toByteArray(args[2]);

        if (StringUtils.equals(args[0], SUBCMD_SEND)) {
            bridge.getLibClient().sendTelegram(data);

        } else if (StringUtils.equals(args[0], "resolve")) {

            List<IEBusCommandMethod> methods = bridge.getLibClient().getClient().getConfigurationProvider()
                    .find(EBusUtils.toByteArray(args[2]));

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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

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