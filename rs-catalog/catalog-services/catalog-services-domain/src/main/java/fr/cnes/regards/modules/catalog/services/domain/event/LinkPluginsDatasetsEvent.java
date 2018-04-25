/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;

/**
 * Published when a {@link LinkUiPluginsDatasets} is created
 *
 * @author Xavier-Alexandre Brochard
 */
@Event(target = Target.ALL)
public class LinkPluginsDatasetsEvent implements ISubscribable {

    private LinkPluginsDatasets linkPluginsDatasets;

    /**
     * Default constructor required by Jackson
     */
    public LinkPluginsDatasetsEvent() {
        super();
    }

    /**
     * @param pLinkPluginsDatasets
     */
    public LinkPluginsDatasetsEvent(LinkPluginsDatasets pLinkPluginsDatasets) {
        super();
        linkPluginsDatasets = pLinkPluginsDatasets;
    }

    /**
     * @return the linkPluginsDatasets
     */
    public LinkPluginsDatasets getLinkPluginsDatasets() {
        return linkPluginsDatasets;
    }

}
