/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

@RestClient(name = "rs-admin", contextId = "rs-admin.access-settings-client")
@RequestMapping(
    path = "/accesses/settings",
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE)
public interface IAccessSettingsClient {

    /**
     * Retrieve the {@link AccessSettings}.
     * @return The {@link AccessSettings}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<EntityModel<AccessSettings>> retrieveAccessSettings();

    /**
     * Update the {@link AccessSettings}.
     * @param accessSettings The {@link AccessSettings}
     * @return The updated access settings
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    ResponseEntity<EntityModel<AccessSettings>> updateAccessSettings(@Valid @RequestBody AccessSettings accessSettings);
}
