/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.service.database.IAIPEntityService;

/**
 * REST controller handling request about {@link AIPEntity}s
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(path = AIPEntityController.BASE_PATH)
public class AIPEntityController implements IResourceController<AIPEntity> {

    /**
     * Controller base path
     */
    static final String BASE_PATH = "sips/{sip_id}/aips";

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * {@link IAIPEntityService} instance
     */
    @Autowired
    private IAIPEntityService aipEntityService;

    /**
     * Retrieve a page of AIPEntities from a given sip id
     * @param sipId
     * @param pageable
     * @param pagedResourcesAssembler
     * @return a page of aip entities
     */
    @ResponseBody
    @ResourceAccess(description = "send pages of AIPEntity")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<AIPEntity>>> retrieveAIPEntities(@PathVariable("sip_id") String sipId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AIPEntity> pagedResourcesAssembler) {
        return new ResponseEntity<>(
                toPagedResources(aipEntityService.retrieveBySip(sipId, pageable), pagedResourcesAssembler),
                HttpStatus.OK);
    }

    @Override
    public Resource<AIPEntity> toResource(AIPEntity pElement, Object... pExtras) {
        Resource<AIPEntity> resource = new Resource<>(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveAIPEntities", LinkRels.LIST,
                                MethodParamFactory.build(String.class), MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        return resource;
    }
}
