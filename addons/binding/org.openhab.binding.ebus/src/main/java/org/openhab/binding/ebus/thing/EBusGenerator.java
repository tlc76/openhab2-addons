package org.openhab.binding.ebus.thing;

import java.util.List;

import de.csdev.ebus.command.EBusCommandCollection;

public interface EBusGenerator {

    public void clear();

    public void update(List<EBusCommandCollection> collections);

}
