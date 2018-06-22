/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.thing;

import java.util.List;

import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.osgi.service.cm.ManagedService;

import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandCollection;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public interface EBusTypeProvider extends ThingTypeProvider, ChannelTypeProvider, ManagedService {

    /**
     * @param collections
     */
    public void update(List<IEBusCommandCollection> collections);

    /**
     * @param listener
     */
    public void addTypeProviderListener(IEBusTypeProviderListener listener);

    /**
     * @param listener
     */
    public void removeTypeProviderListener(IEBusTypeProviderListener listener);

    /**
     * @return
     */
    public EBusCommandRegistry getCommandRegistry();

    /**
     * @return
     */
    public boolean reload();
}
