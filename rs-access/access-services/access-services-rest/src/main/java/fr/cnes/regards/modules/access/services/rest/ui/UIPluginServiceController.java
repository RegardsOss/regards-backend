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
package fr.cnes.regards.modules.access.services.rest.ui;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;

/**
 *
 * Class UIPluginServiceController
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping(UIPluginServiceController.REQUEST_MAPPING_ROOT)
public class UIPluginServiceController implements IResourceController<UIPluginConfiguration> {

    public static final String REQUEST_MAPPING_ROOT = "/services";

    /**
     * Business service to manage {@link UIPluginConfiguration} entities
     */
    @Autowired
    private IUIPluginConfigurationService service;

    /**
     * Service to manage hateoas resources
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Return all generic ui services plus those linked to passed dataset if any given
     *
     * @param datasetId
     *            the id of the {@link Dataset}. Can be <code>null</code>.
     * @return the list of services
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(role = DefaultRole.PUBLIC,
            description = "Return all generic ui services plus those linked to passed dataset if any given")
    public ResponseEntity<List<EntityModel<UIPluginConfiguration>>> retrieveServices(
            @RequestParam(value = "dataset_id", required = false) final String datasetId) {
        final List<UIPluginConfiguration> services = service.retrieveActivePluginServices(datasetId, null);
        return new ResponseEntity<>(toResources(services), HttpStatus.OK);
    }

    @Override
    public EntityModel<UIPluginConfiguration> toResource(final UIPluginConfiguration element, final Object... extras) {
        return resourceService.toResource(element);
    }

}
