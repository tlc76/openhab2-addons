package org.openhab.binding.ebus.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.ebus.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.cfg.ConfigurationReader;
import de.csdev.ebus.cfg.datatypes.EBusTypeException;
import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandCollection;
import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.EBusCommandUtils;
import de.csdev.ebus.command.IEBusCommand.Type;
import de.csdev.ebus.command.IEBusCommandChannel;
import de.csdev.ebus.core.EBusController;
import de.csdev.ebus.core.connection.EBusTCPConnection;
import de.csdev.ebus.service.device.EBusDevice;
import de.csdev.ebus.utils.EBusUtils;

public class EBusLibClient {

    private final Logger logger = LoggerFactory.getLogger(EBusLibClient.class);

    private EBusController controller;

    private EBusClient client;

    private ConfigurationReader reader;

    private EBusTCPConnection connection;

    private List<EBusCommandCollection> collections;

    public void setTCPConnection(String hostname, int port) {
        connection = new EBusTCPConnection(hostname, port);
    }

    public EBusClient getClient() {
        return client;
    }

    public boolean isConnectionValid() {
        return connection != null;
    }

    public ByteBuffer grrr(String commandId, Type type, Thing targetThing) throws EBusTypeException {

        EBusDevice ownDevice = client.getDeviceTable().getOwnDevice();
        // String masterAddress = (String)
        // targetThing.getConfiguration().get(EBusBindingConstants.CONFIG_MASTER_ADDRESS);
        String slaveAddress = (String) targetThing.getConfiguration().get(EBusBindingConstants.CONFIG_SLAVE_ADDRESS);

        IEBusCommandChannel commandChannel = client.getConfigurationProvider().getConfigurationById(commandId, type);

        // TODO: check command type - master slave, master master or broadcast

        if (StringUtils.isEmpty(slaveAddress)) {
            logger.warn("Unable to poll, Thing has no slave address defined!");
            return null;
        }

        byte target = EBusUtils.toByte(slaveAddress);

        return EBusCommandUtils.buildMasterTelegram(commandChannel, ownDevice.getMasterAddress(), target, null);
    }

    public void initClient(Byte masterAddress) {

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

        collections = new ArrayList<>();

        for (Entry<String, String> entry : deviceConfigurations.entrySet()) {
            String configPath = "/commands/" + entry.getKey() + "-configuration.json";
            EBusCommandCollection collection = loadConfiguration(EBusController.class.getResourceAsStream(configPath));
            if (collection != null) {
                collections.add(collection);
            }
        }

    }

    public List<EBusCommandCollection> getConfiguration() {
        return collections;
    }

    public void startClient() {
        controller.start();
    }

    public void stopClient() {
        if (controller != null && !controller.isInterrupted()) {
            controller.interrupt();
        }
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

}
