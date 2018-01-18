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
package fr.cnes.regards.modules.accessrights.client;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;

import org.springframework.hateoas.Resource;
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
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 *
 * Feign client for rs-admin Accesses Rest controller. Thanks to Feign facilities, no exception handling is required.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/accesses", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IRegistrationClient {

    /**
     * Retrieve all access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int pPage,
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
    ResponseEntity<Resource<AccessRequestDto>> requestAccess(@Valid @RequestBody AccessRequestDto pAccessRequest);

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
     * Retrieve the {@link AccountSettings}.
     *
     * @return The {@link AccountSettings}
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    ResponseEntity<Resource<AccessSettings>> getAccessSettings();

    /**
     * Update the {@link AccountSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccountSettings}
     * @return The updated access settings
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody AccessSettings pAccessSettings);
}