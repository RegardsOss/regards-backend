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

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
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

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogServicesController.class);

    public static final String PATH_SERVICES = "/services";

    public static final String PATH_SERVICE_NAME = "/{pluginConfigurationId}/apply";

    @Autowired
    private IServiceManager serviceManager;

    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset.
     * The results are augmented with their <code>applicationModes</code> & <code>entityTypes</code> info via a DTO.
     * <p>If <code>pDatasetId</code> is <code>null</code>, en empty list will be returned
     * <p>If <code>pServiceScope</code> is <code>null</code>, all services on given dataset will be returned, regardless their scope.
     *
     * @param pDatasetId
     *            the id of the {@link Dataset}. Can be <code>null</code>.
     * @param pServiceScope
     *            the applicable mode. Can be <code>null</code>.
     * @return the list of services
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all services applied to given dataset, augmented with meta information",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<Resource<PluginConfigurationDto>>> retrieveServices(
            @RequestParam(value = "dataset_id", required = false) final String pDatasetId,
            @RequestParam(value = "service_scope", required = false) final ServiceScope pServiceScope) {
        LOGGER.error("[XAB] We are on tenant " + runtimeTenantResolver.getTenant());
        final List<PluginConfigurationDto> services = serviceManager.retrieveServices(pDatasetId, pServiceScope);
        return new ResponseEntity<>(HateoasUtils.wrapList(services), HttpStatus.OK);
    }

    /**
     * Apply the given service.
     *
     * @param pPluginConfigurationId
     *            the id of the {@link Dataset}
     * @return whatever is returned by the given service
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST, path = PATH_SERVICE_NAME, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResourceAccess(description = "Apply a given plugin service", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> applyService(
            @PathVariable("pluginConfigurationId") final Long pPluginConfigurationId,
            @RequestBody ServicePluginParameters pServiceParameters, HttpServletResponse response)
            throws ModuleException {
        return serviceManager.apply(pPluginConfigurationId, pServiceParameters, response);
    }

}
