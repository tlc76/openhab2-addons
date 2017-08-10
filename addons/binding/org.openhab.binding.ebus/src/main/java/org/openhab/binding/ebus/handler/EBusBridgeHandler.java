/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.handler;

import static org.openhab.binding.ebus.EBusBindingConstants.CHANNEL_1;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.thing.EBusGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.cfg.ConfigurationReader;
import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandCollection;
import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.core.EBusConnectorEventListener;
import de.csdev.ebus.core.EBusController;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.core.connection.EBusTCPConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
import de.csdev.ebus.service.parser.EBusParserListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 * The {@link EBusBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBridgeHandler extends BaseBridgeHandler implements EBusParserListener, EBusConnectorEventListener {

    private final Logger logger = LoggerFactory.getLogger(EBusBridgeHandler.class);
    private EBusController controller;
    private EBusClient client;
    private ConfigurationReader reader;
    private EBusGenerator generator;

    public EBusBridgeHandler(Bridge bridge, EBusGenerator generator) {
        super(bridge);
        this.generator = generator;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_1)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {

        Configuration configuration = getThing().getConfiguration();

        IEBusConnection connection = null;
        String ipAddress = null;
        BigDecimal port = null;

        String masterAddressStr = null;
        Byte masterAddress = (byte) 0x00;

        try {
            ipAddress = (String) configuration.get("ipAddress");
            port = (BigDecimal) configuration.get("port");
            masterAddressStr = (String) configuration.get("masterAddress");

        } catch (Exception e) {
            logger.debug("Cannot set network parameters.", e);
        }

        if (StringUtils.isNotEmpty(masterAddressStr)) {
            masterAddress = EBusUtils.toByte(masterAddressStr);
        }

        if (StringUtils.isNotEmpty(ipAddress) && port != null) {
            connection = new EBusTCPConnection(ipAddress, port.intValue());
        }

        if (!EBusUtils.isMasterAddress(masterAddress)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "eBus master address is not a valid master address!");

            return;
        }

        if (connection == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Network address and port must be set!");

            return;
        }

        // load the eBus core element
        controller = new EBusController(connection);

        // load the high level client
        client = new EBusClient(controller, masterAddress);

        reader = new ConfigurationReader();
        reader.setEBusTypes(client.getDataTypes());

        HashMap<String, String> deviceConfigurations = new HashMap<>();
        deviceConfigurations.put("common", "eBus Standard");
        deviceConfigurations.put("wolf-cgb2", "Wolf CGB2");
        deviceConfigurations.put("wolf-sm1", "Wolf SM1");

        List<EBusCommandCollection> collections = new ArrayList<>();

        for (Entry<String, String> entry : deviceConfigurations.entrySet()) {
            String configPath = "/commands/" + entry.getKey() + "-configuration.json";
            EBusCommandCollection collection = loadConfiguration(EBusController.class.getResourceAsStream(configPath));
            if (collection != null) {
                collections.add(collection);
            }
        }

        generator.update(collections);

        // loadConfiguration(EBusController.class.getResourceAsStream("/commands/common-configuration.json"));
        // loadConfiguration(EBusController.class.getResourceAsStream("/commands/wolf-cgb2-configuration.json"));
        // List<IEBusCommand> configurationList = client.getConfigurationProvider().getConfigurationList();
        // generator.update(configurationList);

        // controller.addEBusEventListener(this);
        client.getResolverService().addEBusParserListener(this);

        updateStatus(ThingStatus.ONLINE);

        // start eBus controller
        // controller.start();
    }

    private EBusCommandCollection loadConfiguration(InputStream is) {

        EBusCommandCollection collection = null;
        try {

            EBusCommandRegistry provider = client.getConfigurationProvider();

            collection = reader.loadConfigurationCollection(is);
            provider.addTelegramConfigurationList(collection.getCommands());

        } catch (IOException e) {
            logger.error("error!", e);
        }

        return collection;
    }

    @Override
    public void dispose() {
        if (controller != null && !controller.isInterrupted()) {
            controller.interrupt();
        }

        if (client != null) {
            client = null;
        }
    }

    private void updateValue() {
        // updateState(new ChannelUID(getThing().getUID(), "CHANNEL_TEMPERATURE"), getTemperature());
    }

    @Override
    public void onTelegramResolved(IEBusCommand command, Map<String, Object> result, byte[] receivedData,
            Integer sendQueueId) {

        for (Entry<String, Object> resultEntry : result.entrySet()) {
            // command.getMasterTypes().
        }

        // ChannelTypeUID uid = new ChannelTypeUID(
        // BINDING_ID + ":" + command.getId().replace('.', ':') + ":" + value.getName());
    }

    @Override
    public void onTelegramReceived(byte[] receivedData, Integer sendQueueId) {
        if (EBusBridgeHandler.this.getBridge().getStatus().equals(ThingStatus.INITIALIZING)) {
            // set status to online if we are able to receive valid telegrams
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void onTelegramException(EBusDataException exception, Integer sendQueueId) {
        // noop
    }

    @Override
    public void onConnectionException(Exception e) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    }
}
