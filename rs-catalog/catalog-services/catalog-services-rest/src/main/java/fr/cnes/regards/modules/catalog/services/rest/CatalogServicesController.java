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
package fr.cnes.regards.modules.catalog.services.rest;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import fr.cnes.regards.modules.catalog.services.service.IServiceManager;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

/**
 * REST Controller handling operations on services.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping(CatalogServicesController.PATH_SERVICES)
public class CatalogServicesController {

    public static final String PATH_SERVICES = "/services";

    public static final String PATH_SERVICE_NAME = "/{pluginConfigurationBusinessId}/apply";

    public static final String DATASET_IDS_QUERY_PARAM = "datasetIpIds";

    public static final String SCOPES_QUERY_PARAM = "applicationModes";

    /**
     * {@link IServiceManager} instance
     */
    @Autowired
    private IServiceManager serviceManager;

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset.
     * The results are augmented with their <code>applicationModes</code> & <code>entityTypes</code> info via a DTO.
     * <p>If <code>pDatasetId</code> is <code>null</code>, en empty list will be returned
     * <p>If <code>pServiceScope</code> is <code>null</code>, all services on given dataset will be returned, regardless their scope.
     *
     * @param pDatasetIds    the id of the {@link Dataset}. Can be <code>null</code>.
     * @param pServiceScopes the applicable mode. Can be <code>null</code>.
     * @return the list of services
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all services applied to given dataset, augmented with meta information",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<List<EntityModel<PluginConfigurationDto>>> retrieveServices(
        @RequestParam(value = DATASET_IDS_QUERY_PARAM, required = false) final List<String> pDatasetIds,
        @RequestParam(value = SCOPES_QUERY_PARAM, required = false) final List<ServiceScope> pServiceScopes) {
        final List<PluginConfigurationDto> services = serviceManager.retrieveServices(pDatasetIds, pServiceScopes);
        return new ResponseEntity<>(HateoasUtils.wrapList(services), HttpStatus.OK);
    }

    /**
     * Apply the given service.
     *
     * @param pluginConfigurationBusinessId the id of the {@link Dataset}
     * @return whatever is returned by the given service
     */
    @RequestMapping(method = RequestMethod.POST, path = PATH_SERVICE_NAME, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResourceAccess(description = "Apply a given plugin service", role = DefaultRole.PUBLIC)
    public ResponseEntity<StreamingResponseBody> applyService(
        @PathVariable("pluginConfigurationBusinessId") final String pluginConfigurationBusinessId,
        @RequestBody ServicePluginParameters pServiceParameters,
        HttpServletResponse response) throws ModuleException {
        return serviceManager.apply(pluginConfigurationBusinessId, pServiceParameters, response);
    }

}
