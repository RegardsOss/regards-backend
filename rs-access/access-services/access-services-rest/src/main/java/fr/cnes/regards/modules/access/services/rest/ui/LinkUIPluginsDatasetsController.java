/*
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
package fr.cnes.regards.modules.access.services.rest.ui;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.service.link.ILinkUIPluginsDatasetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Class LinkUIPluginsDatasetsController
 * <p>
 * Rest controller to link a dataset to one or many UIPluginConfiguration
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping(path = LinkUIPluginsDatasetsController.REQUEST_MAPPING_ROOT)
public class LinkUIPluginsDatasetsController {

    public static final String REQUEST_MAPPING_ROOT = "/linkuiplugindataset/{datasetId}";

    @Autowired
    private ILinkUIPluginsDatasetsService linkService;

    /**
     * The resource service. Autowired by Spring.
     */
    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "endpoint allowing to retrieve which plugins are to be applied to a given dataset",
                    role = DefaultRole.PROJECT_ADMIN)
    @ResponseBody
    public ResponseEntity<EntityModel<LinkUIPluginsDatasets>> retrieveLink(
        @PathVariable("datasetId") final String datasetId) throws EntityNotFoundException {
        final LinkUIPluginsDatasets link = linkService.retrieveLink(datasetId);
        EntityModel<LinkUIPluginsDatasets> resource = resourceService.toResource(link);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "endpoint allowing to modify which plugins are to be applied to a given dataset",
                    role = DefaultRole.PROJECT_ADMIN)
    @ResponseBody
    public ResponseEntity<EntityModel<LinkUIPluginsDatasets>> updateLink(
        @PathVariable("datasetId") final String datasetId, @RequestBody final LinkUIPluginsDatasets updatedLink)
        throws EntityException {
        final LinkUIPluginsDatasets link = linkService.updateLink(datasetId, updatedLink);
        EntityModel<LinkUIPluginsDatasets> resource = resourceService.toResource(link);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

}
