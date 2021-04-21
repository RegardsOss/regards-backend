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

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin", contextId = "rs-admin.ms-resources-client")
@RequestMapping(value = IMicroserviceResourceClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IMicroserviceResourceClient {

    /**
     * Controller base mapping
     */
    String TYPE_MAPPING = "/resources/microservices/{microservicename}";

    /**
     * Root to retreive resources by microservice and controller name
     */
    String CONTROLLERS_MAPPING = "/controllers";

    /**
     * Root to retreive resources by microservice and controller name
     */
    String CONTROLLER_MAPPING = CONTROLLERS_MAPPING + "/{controllername}";

    /**
     * Retrieve the resource accesses available to the user of the given microservice
     *
     * @param pMicroserviceName
     *            microservice
     * @param pPageable
     *            pagination information
     * @param pPagedResourcesAssembler
     *            page assembler
     * @return list of user resource accesses for given microservice
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<ResourcesAccess>>> getAllResourceAccessesByMicroservice(
            @PathVariable("microservicename") final String pMicroserviceName, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     *
     * @param pMicroserviceName
     *            microservice name
     * @param pResourcesToRegister
     *            resource to register for the specified microservice
     * @return {@link Void}
     */
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Void> registerMicroserviceEndpoints(@PathVariable("microservicename") final String pMicroserviceName,
            @RequestBody @Valid final List<ResourceMapping> pResourcesToRegister);

    /**
     * Retrieve all resource controller names for the given microservice.
     *
     * @param pMicroserviceName
     *            microservice
     * @return list of all controllers associated to the specified microservice
     */
    @RequestMapping(method = RequestMethod.GET, value = CONTROLLERS_MAPPING)
    ResponseEntity<List<String>> retrieveMicroserviceControllers(
            @PathVariable("microservicename") final String pMicroserviceName);

    /**
     * Retrieve all resources for the given microservice and the given controller name
     *
     * @param pMicroserviceName
     *            microservice
     * @param pControllerName
     *            controller
     * @return List of accessible resources for the specified microservice and controller
     */
    @RequestMapping(method = RequestMethod.GET, value = CONTROLLER_MAPPING)
    ResponseEntity<List<EntityModel<ResourcesAccess>>> retrieveMicroserviceControllerEndpoints(
            @PathVariable("microservicename") final String pMicroserviceName,
            @PathVariable("controllername") final String pControllerName);

}
