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
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;

/**
 * {@link MetaProduct} REST module controller
 * 
 * @author Christophe Mertz
 *
 */
@RestController
@ModuleInfo(name = "mateproducts", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(path = MetaProductController.BASE_PATH)
public class MetaProductController implements IResourceController<MetaProduct> {

    public static final String BASE_PATH = "metaproducts";

    /**
     * Business service for {@link MetaProduct}
     */
    @Autowired
    private IMetaProductService metaproductService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link MetaProduct}
     * @param pageable a {@link Pageable} for pagination information
     * @param assembler a {@link ResourceAssembler} to easily convert {@link Page} instances into {@link PagedResources}
     * @return {@link List} of {@link Resource} of {@link MetaProduct}
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "List all the metaproducts", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<MetaProduct>>> retrieveAll(final Pageable pageable,
            final PagedResourcesAssembler<MetaProduct> assembler) {
        return new ResponseEntity<>(toPagedResources(metaproductService.retrieveAll(pageable), assembler),
                HttpStatus.OK);
    }

    /**
     * Get a {@link MetaProduct}
     * @param metaproductId the {@link MetaProduct} identifier
     * @return the retrieved {@link MetaProduct}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{metaproductId}")
    @ResourceAccess(description = "Get a metaproduct", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<MetaProduct>> get(@PathVariable final Long metaproductId) throws ModuleException {
        return ResponseEntity.ok(toResource(metaproductService.retrieve(metaproductId)));
    }

    /**
     * Create a {@link MetaProduct}
     * @param metaproduct the {@link MetaProduct} to create
     * @return the created {@link MetaProduct}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "Add a metaproduct", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<MetaProduct>> save(@Valid @RequestBody MetaProduct metaproduct)
            throws ModuleException {
        return new ResponseEntity<>(toResource(metaproductService.createOrUpdate(metaproduct)), HttpStatus.CREATED);
    }

    /**
     * Update a {@link MetaProduct} 
     * @param metaproductId the {@link MetaProduct} identifier to update
     * @param metaproduct the {@link MetaProduct} to update
     * @return the updated {@link MetaProduct}
     * @throws ModuleException if error occurs!
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{metaproductId}")
    @ResponseBody
    @ResourceAccess(description = "Update a metaproduct", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<MetaProduct>> update(@PathVariable final Long metaproductId,
            @Valid @RequestBody MetaProduct metaproduct) throws ModuleException {
        return ResponseEntity.ok(toResource(metaproductService.update(metaproductId, metaproduct)));
    }

    @Override
    public Resource<MetaProduct> toResource(MetaProduct element, Object... extras) {
        Resource<MetaProduct> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveAll", LinkRels.LIST);
        resourceService.addLink(resource, this.getClass(), "get", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(MetaProduct.class));
        return resource;
    }

}
