/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.access.services.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;

/**
 * Published when a {@link UIPluginConfiguration} is created or updated
 *
 * @author Xavier-Alexandre Brochard
 */
@Event(target = Target.ALL)
public class UIPluginConfigurationEvent implements ISubscribable {

    private UIPluginConfiguration uiPluginConfiguration;

    /**
     * Default constructor required by Jackson
     */
    public UIPluginConfigurationEvent() {
        super();
    }

    /**
     * @param uiPluginConfiguration Source object of the event
     */
    public UIPluginConfigurationEvent(UIPluginConfiguration uiPluginConfiguration) {
        super();
        this.uiPluginConfiguration = uiPluginConfiguration;
    }

    /**
     * @return the uiPluginConfiguration
     */
    public UIPluginConfiguration getUiPluginConfiguration() {
        return uiPluginConfiguration;
    }

}
