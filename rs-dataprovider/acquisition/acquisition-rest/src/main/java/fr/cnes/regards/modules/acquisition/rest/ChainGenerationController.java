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
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;

/**
 * {@link ChainGeneration} REST module controller
 * 
 * @author Christophe Mertz
 *
 */
@RestController
@ModuleInfo(name = "chains", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(path = ChainGenerationController.BASE_PATH)
public class ChainGenerationController implements IResourceController<ChainGeneration> {

    public static final String BASE_PATH = "chains";

    /**
     * Business service for {@link ChainGeneration}
     */
    @Autowired
    private IChainGenerationService chainService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link ChainGeneration}
     * @param pageable a {@link Pageable} for pagination information
     * @param assembler a {@link ResourceAssembler} to easily convert {@link Page} instances into {@link PagedResources}
     * @return {@link List} of {@link Resource} of {@link ChainGeneration}
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "List all the chains", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<ChainGeneration>>> retrieveAll(final Pageable pageable,
            final PagedResourcesAssembler<ChainGeneration> assembler) {
        return new ResponseEntity<>(toPagedResources(chainService.retrieveAll(pageable), assembler), HttpStatus.OK);
    }

    /**
     * Get a {@link ChainGeneration}
     * @param chainId the {@link ChainGeneration} identifier
     * @return the retrieved {@link ChainGeneration}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{chainId}")
    @ResourceAccess(description = "Get a chain", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ChainGeneration>> get(@PathVariable final Long chainId) throws ModuleException {
        return ResponseEntity.ok(toResource(chainService.retrieve(chainId)));
    }

    /**
     * Create a {@link ChainGeneration}
     * @param chainGeneration the {@link ChainGeneration} to create
     * @return the created {@link ChainGeneration}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "Add a chain", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ChainGeneration>> create(@Valid @RequestBody ChainGeneration chainGeneration)
            throws ModuleException {
        return new ResponseEntity<>(toResource(chainService.createOrUpdate(chainGeneration)), HttpStatus.CREATED);
    }

    /**
     * Update a {@link ChainGeneration} 
     * @param chainId the {@link ChainGeneration} identifier to update
     * @param chainGeneration the {@link ChainGeneration} to update
     * @return the updated {@link ChainGeneration}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{chainId}")
    @ResponseBody
    @ResourceAccess(description = "Update a chain", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ChainGeneration>> update(@PathVariable final Long chainId,
            @Valid @RequestBody ChainGeneration chainGeneration) throws ModuleException {
        return ResponseEntity.ok(toResource(chainService.update(chainId, chainGeneration)));
    }

    @Override
    public Resource<ChainGeneration> toResource(ChainGeneration element, Object... extras) {
        Resource<ChainGeneration> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAll", LinkRels.LIST);
        resourceService.addLink(resource, this.getClass(), "get", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(ChainGeneration.class));
        return resource;
    }

}
