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
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;

/**
 * @author sbinda
 *
 */
@Event(target = Target.ALL)
public class UIPluginDefinitionEvent implements ISubscribable {

    private UIPluginDefinition pluginDefinition;

    private EventType type;

    public static UIPluginDefinitionEvent build(UIPluginDefinition pluginDefinition, EventType type) {
        UIPluginDefinitionEvent event = new UIPluginDefinitionEvent();
        event.pluginDefinition = pluginDefinition;
        event.type = type;
        return event;
    }

    public UIPluginDefinition getPluginDefinition() {
        return pluginDefinition;
    }

    public EventType getType() {
        return type;
    }

}
