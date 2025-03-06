/*

 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.downloader.service;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Event to inform other services that a storage plugin conf has changed.
 * Used to inform downloader service who needs to update its plugins cache as it share the same plugins.
 *
 * @author Sébastien Binda
 **/
@Event(target = Target.ALL, converter = JsonMessageConverter.GSON)
public class StoragePluginConfEvent implements ISubscribable {

    /**
     * Business id of the changed plugin
     */
    private final String businessId;

    public StoragePluginConfEvent(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessId() {
        return businessId;
    }
}
