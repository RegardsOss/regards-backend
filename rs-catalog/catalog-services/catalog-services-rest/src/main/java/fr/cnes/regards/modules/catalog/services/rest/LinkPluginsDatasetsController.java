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
package fr.cnes.regards.modules.catalog.services.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.catalog.services.service.link.ILinkPluginsDatasetsService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@RequestMapping(path = LinkPluginsDatasetsController.PATH_LINK)
public class LinkPluginsDatasetsController {

    /**
     * Controller base path
     */
    public static final String PATH_LINK = "/linkplugindataset/{datasetId}";

    /**
     * {@link ILinkPluginsDatasetsService} instance
     */
    @Autowired
    private ILinkPluginsDatasetsService linkService;

    /**
     * The resource service. Autowired by Spring.
     */
    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "endpoint allowing to retrieve which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<EntityModel<LinkPluginsDatasets>> retrieveLink(
            @PathVariable("datasetId") final String pDatasetId) throws EntityNotFoundException {
        final LinkPluginsDatasets link = linkService.retrieveLink(pDatasetId);
        EntityModel<LinkPluginsDatasets> resource = resourceService.toResource(link);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "endpoint allowing to modify which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<EntityModel<LinkPluginsDatasets>> updateLink(
            @PathVariable("datasetId") final String pDatasetId, @RequestBody final LinkPluginsDatasets pUpdatedLink)
            throws EntityException {
        final LinkPluginsDatasets link = linkService.updateLink(pDatasetId, pUpdatedLink);
        EntityModel<LinkPluginsDatasets> resource = resourceService.toResource(link);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

}
