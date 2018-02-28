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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;

@RestController
@RequestMapping(IngestProcessingChainController.TYPE_MAPPING)
public class IngestProcessingChainController implements IResourceController<IngestProcessingChain> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingChainController.class);

    public static final String TYPE_MAPPING = "/processingchains";

    public static final String NAME_PATH = "/{name}";

    public static final String IMPORT_PATH = "/import";

    public static final String EXPORT_PATH = NAME_PATH + "/export";

    @Value("${spring.application.name}")
    private String applicationName;

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

    @ResourceAccess(description = "Create a new ingestion processing chain")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Resource<IngestProcessingChain>> create(
            @Valid @RequestBody IngestProcessingChain processingChain) throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.createNewChain(processingChain);
        return new ResponseEntity<>(toResource(chain), HttpStatus.CREATED);

    }

    @ResourceAccess(description = "Create a new ingestion processing chain importing JSON file")
    @RequestMapping(method = RequestMethod.POST, value = IMPORT_PATH)
    public ResponseEntity<Resource<IngestProcessingChain>> createByFile(@RequestParam("file") MultipartFile file)
            throws ModuleException {
        try {
            IngestProcessingChain chain = ingestProcessingService.createNewChain(file.getInputStream());
            return new ResponseEntity<>(toResource(chain), HttpStatus.CREATED);
        } catch (IOException e) {
            final String message = "Error with file stream while importing ingest processing chain.";
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    @ResourceAccess(description = "Export an ingestion processing chain in JSON format")
    @RequestMapping(method = RequestMethod.GET, value = EXPORT_PATH)
    public void export(HttpServletRequest pRequest, HttpServletResponse pResponse, @PathVariable("name") String name)
            throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.getChain(name);
        String exportedFilename = applicationName + "-" + chain.getName() + ".json";

        // Produce octet stream to force navigator opening "save as" dialog
        pResponse.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        pResponse.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFilename + "\"");

        try {
            ingestProcessingService.exportProcessingChain(name, pResponse.getOutputStream());
            // FIXME maybe already done!
            pResponse.getOutputStream().flush();
        } catch (IOException e) {
            String message = String.format(
                                           "Error with servlet output stream while exporting ingest processing chain %s.",
                                           chain.getName());
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    @ResourceAccess(description = "Update an existing IngestProcessingChain.")
    @RequestMapping(value = NAME_PATH, method = RequestMethod.PUT)
    public ResponseEntity<Resource<IngestProcessingChain>> update(@PathVariable("name") String name,
            @Valid @RequestBody IngestProcessingChain processingChain) throws ModuleException {
        if (!name.equals(processingChain.getName())) {
            throw new EntityInvalidException("Name of entity to update does not match.");
        }
        IngestProcessingChain chain = ingestProcessingService.updateChain(processingChain);
        return new ResponseEntity<>(toResource(chain), HttpStatus.OK);
    }

    @Override
    public Resource<IngestProcessingChain> toResource(IngestProcessingChain ingestChain, Object... pExtras) {
        final Resource<IngestProcessingChain> resource = resourceService.toResource(ingestChain);
        resourceService.addLink(resource, this.getClass(), "get", LinkRels.SELF,
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        if (!ingestChain.getName().equals(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL)) {
            resourceService.addLink(resource, this.getClass(), "delete", LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, ingestChain.getName()));
        }
        resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, ingestChain.getName()),
                                MethodParamFactory.build(IngestProcessingChain.class));
        resourceService.addLink(resource, this.getClass(), "export", "export",
                                MethodParamFactory.build(HttpServletRequest.class),
                                MethodParamFactory.build(HttpServletResponse.class),
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        return resource;
    }

}
