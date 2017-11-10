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
package fr.cnes.regards.modules.ingest.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;

@RestController
@ModuleInfo(name = "IngestProcessingChainController", description = "Manage processing chain for ingestion",
        version = "2.0.0-SNAPSHOT", author = "CSSI", legalOwner = "CNES",
        documentation = "https://github.com/RegardsOss")
@RequestMapping(IngestProcessingChainController.TYPE_MAPPING)
public class IngestProcessingChainController implements IResourceController<IngestProcessingChain> {

    public static final String TYPE_MAPPING = "/processingchains";

    public static final String NAME_PATH = "/{name}";

    @Autowired
    private IIngestProcessingService ingestProcessingService;

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    @ResourceAccess(description = "Search for IngestProcessingChain with optional criterion.")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<IngestProcessingChain>>> search(
            @RequestParam(name = "name", required = false) String name, Pageable pageable,
            PagedResourcesAssembler<IngestProcessingChain> pAssembler) {
        Page<IngestProcessingChain> chains = ingestProcessingService.searchChains(name, pageable);
        PagedResources<Resource<IngestProcessingChain>> resources = toPagedResources(chains, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve an IngestProcessingChain by name.")
    @RequestMapping(value = NAME_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource<IngestProcessingChain>> get(@PathVariable("name") String name)
            throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.getChain(name);
        return new ResponseEntity<>(toResource(chain), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete an IngestProcessingChain by name.")
    @RequestMapping(value = NAME_PATH, method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(@PathVariable("name") String name) throws ModuleException {
        ingestProcessingService.deleteChain(name);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Create a new IngestProcessingChain.")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Resource<IngestProcessingChain>> create(
            @Valid @RequestBody IngestProcessingChain processingChain) throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.createNewChain(processingChain);
        return new ResponseEntity<>(toResource(chain), HttpStatus.CREATED);

    }

    @ResourceAccess(description = "Update an existing IngestProcessingChain.")
    @RequestMapping(value = NAME_PATH, method = RequestMethod.PUT)
    public ResponseEntity<Resource<IngestProcessingChain>> update(
            @Valid @RequestBody IngestProcessingChain processingChain) throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.updateChain(processingChain);
        return new ResponseEntity<>(toResource(chain), HttpStatus.OK);
    }

    @Override
    public Resource<IngestProcessingChain> toResource(IngestProcessingChain ingestChain, Object... pExtras) {
        final Resource<IngestProcessingChain> resource = resourceService.toResource(ingestChain);
        resourceService.addLink(resource, this.getClass(), "get", LinkRels.SELF,
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        resourceService.addLink(resource, this.getClass(), "delete", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        return resource;
    }

}
