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
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;

/**
 * Published when a {@link LinkUIPluginsDatasets} is created
 *
 * @author Xavier-Alexandre Brochard
 */
@Event(target = Target.MICROSERVICE)
public class LinkUiPluginsDatasetsEvent implements ISubscribable {

    private LinkUIPluginsDatasets linkUIPluginsDatasets;

    /**
     * Default constructor required by Jackson
     */
    public LinkUiPluginsDatasetsEvent() {
        super();
    }

    /**
     * @param linkUIPluginsDatasets Source object of the event
     */
    public LinkUiPluginsDatasetsEvent(LinkUIPluginsDatasets linkUIPluginsDatasets) {
        super();
        this.linkUIPluginsDatasets = linkUIPluginsDatasets;
    }

    /**
     * @return the linkUIPluginsDatasets
     */
    public LinkUIPluginsDatasets getLinkUIPluginsDatasets() {
        return linkUIPluginsDatasets;
    }

}
