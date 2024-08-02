package fr.cnes.regards.modules.backendforfrontend.rest;/*
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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.model.dto.event.AttributeCacheRefreshEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = AttributeCacheRefreshController.ROOT_PATH)
public class AttributeCacheRefreshController {

    public static final String ROOT_PATH = "/attribute/cache";

    @Autowired
    private IPublisher publisher;

    @DeleteMapping
    @ResourceAccess(description = "Allows to manually refresh model attribute caches (mainly for serialization issues)",
                    role = DefaultRole.ADMIN)
    public void refreshAttributeCache() {
        publisher.publish(new AttributeCacheRefreshEvent());
    }
}
