/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.thing;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.osgi.service.cm.ManagedService;

import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandCollection;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public interface EBusTypeProvider
        extends ThingTypeProvider, ChannelTypeProvider, ChannelGroupTypeProvider, ManagedService {

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

    /**
     * @see ChannelTypeRegistry#getChannelGroupType(ChannelGroupTypeUID, Locale)
     */
    @Override
    @Nullable
    ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, @Nullable Locale locale);

    /**
     * @see ChannelTypeRegistry#getChannelGroupTypes(Locale)
     */
    @Override
    @Nullable
    Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale);
}
