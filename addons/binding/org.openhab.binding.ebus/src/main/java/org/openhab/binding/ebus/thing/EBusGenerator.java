package org.openhab.binding.ebus.thing;

import java.util.List;

import de.csdev.ebus.command.IEBusCommand;

public interface EBusGenerator {

    public void update(List<IEBusCommand> configurationList);

}
