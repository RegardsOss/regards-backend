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
package fr.cnes.regards.modules.acquisition.rest;

import javax.validation.Valid;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import antlr.collections.List;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;

/**
 * {@link MetaFile} REST module controller
 * 
 * @author Christophe Mertz
 *
 */
@RestController
@ModuleInfo(name = "metafiles", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(path = MetaFileController.BASE_PATH)
public class MetaFileController implements IResourceController<MetaFile> {

    /**
     * Controller base path
     */
    public static final String BASE_PATH = "metafiles";

    /**
     * Business service for {@link MetaFile}
     */
    @Autowired
    private IMetaFileService metafileService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link MetaFile}
     * @param pageable a {@link Pageable} for pagination information
     * @param assembler a {@link ResourceAssembler} to easily convert {@link Page} instances into {@link PagedResources}
     * @return {@link List} of {@link Resource} of {@link MetaFile}
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "List all the metafiles", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<MetaFile>>> retrieveAll(final Pageable pageable,
            final PagedResourcesAssembler<MetaFile> assembler) {
        return new ResponseEntity<>(toPagedResources(metafileService.retrieveAll(pageable), assembler), HttpStatus.OK);
    }

    /**
     * Get a {@link MetaFile}
     * @param metafileId the {@link MetaFile} identifier
     * @return the retrieved {@link MetaFile}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{metafileId}")
    @ResourceAccess(description = "Get a metafile", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<MetaFile>> get(@PathVariable final Long metafileId) throws ModuleException {
        return ResponseEntity.ok(toResource(metafileService.retrieve(metafileId)));
    }

    /**
     * Create a {@link MetaFile}
     * @param metafile the {@link MetaFile} to create
     * @return the created {@link MetaFile}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "Add a metafile", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<MetaFile>> save(@Valid @RequestBody MetaFile metafile) throws ModuleException {
        return new ResponseEntity<>(toResource(metafileService.createOrUpdate(metafile)), HttpStatus.CREATED);
    }

    /**
     * Update a {@link MetaFile} 
     * @param metafileId the {@link MetaFile} identifier to update
     * @param metafile the {@link MetaFile} to update
     * @return the updated {@link MetaFile}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{metafileId}")
    @ResponseBody
    @ResourceAccess(description = "Update a metafile", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<MetaFile>> update(@PathVariable final Long metafileId,
            @Valid @RequestBody MetaFile metafile) throws ModuleException {
        return ResponseEntity.ok(toResource(metafileService.update(metafileId, metafile)));
    }

    @Override
    public Resource<MetaFile> toResource(MetaFile element, Object... extras) {
        Resource<MetaFile> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAll", LinkRels.LIST);
        resourceService.addLink(resource, this.getClass(), "get", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(MetaFile.class));
        return resource;
    }

}
