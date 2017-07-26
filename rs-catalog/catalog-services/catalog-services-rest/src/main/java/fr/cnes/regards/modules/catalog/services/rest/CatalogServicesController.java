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
package fr.cnes.regards.modules.catalog.services.rest;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.catalog.services.domain.IService;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.service.IServiceManager;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * REST Controller handling operations on services.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping(CatalogServicesController.PATH_SERVICES)
public class CatalogServicesController {

    public static final String PATH_SERVICES = "/services/{dataset_id}";

    public static final String PATH_META = "/meta";

    public static final String PATH_SERVICE_NAME = "/{service_name}";

    @Autowired
    private IServiceManager serviceManager;

    /**
     * Retrieve all services configured for a dataset and a given scope
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}
     * @param pServiceScope
     *            the {@link ServiceScope}
     * @return the list of services configured for the given dataset and the given scope
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(
            description = "endpoint allowing to retrieve all services configured for a dataset and a given scope")
    public ResponseEntity<Set<PluginConfiguration>> retrieveServices(
            @PathVariable("dataset_id") final String pDatasetId,
            @RequestParam("service_scope") final ServiceScope pServiceScope) throws EntityNotFoundException {
        final Set<PluginConfiguration> services = serviceManager.retrieveServices(pDatasetId, pServiceScope);
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset, add applicationModes
     * & entityTypes info via a DTO
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}
     * @return the list of services
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, path = PATH_META)
    @ResourceAccess(description = "Retrieve all services applied to given dataset, augmented with meta information")
    public ResponseEntity<Collection<Resource<PluginConfigurationDto>>> retrieveServicesWithMeta(
            @PathVariable("dataset_id") final String pDatasetId) throws EntityNotFoundException {
        final Set<PluginConfigurationDto> services = serviceManager.retrieveServicesWithMeta(pDatasetId);
        return new ResponseEntity<>(HateoasUtils.wrapCollection(services), HttpStatus.OK);
    }

    /**
     * Apply the given service.
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}
     * @param pServiceName
     *            the {@link PluginConfiguration}'s label to be executed
     * @param pQuery
     *            the query to be interpreted to get the objects on which apply the service
     * @param pQueryParameters
     *            the query parameters
     * @return whatever is returned by the given service
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET, path = PATH_SERVICE_NAME)
    @ResourceAccess(
            description = "endpoint allowing to apply the given service on objects retrieved thanks to the given query")
    public ResponseEntity<?> applyService(@PathVariable("dataset_id") final String pDatasetId,
            @PathVariable("service_name") final String pServiceName,
            @RequestParam final Map<String, String> pQueryParameters) throws ModuleException {
        return serviceManager.apply(pDatasetId, pServiceName, pQueryParameters);
    }

}
