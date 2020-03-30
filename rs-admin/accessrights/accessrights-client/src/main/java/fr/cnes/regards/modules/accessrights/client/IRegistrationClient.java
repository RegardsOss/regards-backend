/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 *
 * Feign client for rs-admin Accesses Rest controller. Thanks to Feign facilities, no exception handling is required.
 *
 * @author CS

 */
@RestClient(name = "rs-admin", contextId = "rs-admin.registration-client")
@RequestMapping(value = "/accesses", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IRegistrationClient {

    /**
     * Retrieve all access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<EntityModel<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     * Request a new access, i.e. a new project user
     *
     * @param pAccessRequest
     *            A Dto containing all information for creating a the new project user
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<EntityModel<AccessRequestDto>> requestAccess(@Valid @RequestBody AccessRequestDto pAccessRequest);

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     *
     * @param pAccessRequest
     *            A Dto containing all information for creating a the new project user
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(value = "/external", method = RequestMethod.POST)
    ResponseEntity<EntityModel<AccessRequestDto>> requestExternalAccess(
            @Valid @RequestBody AccessRequestDto pAccessRequest);

    /**
     * Grants access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{access_id}/accept", method = RequestMethod.PUT)
    ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Denies access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}/deny", method = RequestMethod.PUT)
    ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Rejects the access request
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Retrieve the {@link AccessSettings}.
     *
     * @return The {@link AccessSettings}
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    ResponseEntity<EntityModel<AccessSettings>> getAccessSettings();

    /**
     * Update the {@link AccessSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccessSettings}
     * @return The updated access settings
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody AccessSettings pAccessSettings);
}