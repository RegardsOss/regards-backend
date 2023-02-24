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
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static fr.cnes.regards.modules.accessrights.client.IRegistrationClient.TARGET_NAME;

/**
 * Feign client for rs-admin Accesses Rest controller. Thanks to Feign facilities, no exception handling is required.
 *
 * @author CS
 */
@RestClient(name = TARGET_NAME, contextId = "rs-admin.registration-client")
public interface IRegistrationClient {

    String ROOT_PATH = "/accesses";

    String TARGET_NAME = "rs-admin";

    /**
     * Request a new access, i.e. a new project user
     *
     * @param pAccessRequest A Dto containing all information for creating a new project user
     * @return the passed Dto
     */
    @PostMapping(value = ROOT_PATH + ROOT_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<AccessRequestDto>> requestAccess(@Valid @RequestBody AccessRequestDto pAccessRequest);

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     *
     * @param pAccessRequest A Dto containing all information for creating a new project user
     * @return the passed Dto
     */
    @PostMapping(value = ROOT_PATH + "/external",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<AccessRequestDto>> requestExternalAccess(@Valid @RequestBody
                                                                        AccessRequestDto pAccessRequest);

    /**
     * Confirm the registration by email.
     *
     * @param token the token
     * @return void
     */
    @GetMapping(value = ROOT_PATH + "/verifyEmail/{token}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> verifyEmail(@PathVariable("token") final String token);

    /**
     * Grants access to the project user
     *
     * @param pAccessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(value = ROOT_PATH + "/{access_id}/accept",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Denies access to the project user
     *
     * @param pAccessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(value = ROOT_PATH + "/{access_id}/deny",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Activates an inactive user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(value = ROOT_PATH + "/{access_id}/active",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> activeAccess(@PathVariable("access_id") Long accessId);

    /**
     * Deactivates an active user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(value = ROOT_PATH + "/{access_id}/inactive",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> inactiveAccess(@PathVariable("access_id") Long accessId);

    /**
     * Rejects the access request
     *
     * @param pAccessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @DeleteMapping(value = ROOT_PATH + "/{access_id}",
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") Long pAccessId);

}