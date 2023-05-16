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
package fr.cnes.regards.modules.ingest.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping(IngestProcessingChainController.TYPE_MAPPING)
public class IngestProcessingChainController implements IResourceController<IngestProcessingChain> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingChainController.class);

    public static final String TYPE_MAPPING = "/processingchains";

    public static final String REQUEST_PARAM_NAME = "name";

    public static final String NAME_PATH = "/{" + REQUEST_PARAM_NAME + "}";

    public static final String IMPORT_PATH = "/import";

    public static final String EXPORT_PATH = NAME_PATH + "/export";

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private IIngestProcessingChainService ingestProcessingService;

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get ingest processing chains", description = "Return a page of ingest processing chains")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All ingest processing chains were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve all ingest processing chains, matching provided name when provided.",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<IngestProcessingChain>>> search(
        @RequestParam(name = "name", required = false) String name,
        @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<IngestProcessingChain> pAssembler) {

        return new ResponseEntity<>(toPagedResources(ingestProcessingService.searchChains(name, pageable), pAssembler),
                                    HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve an IngestProcessingChain by name.", role = DefaultRole.EXPLOIT)
    @GetMapping(value = NAME_PATH)
    public ResponseEntity<EntityModel<IngestProcessingChain>> get(@PathVariable("name") String name)
        throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.getChain(name);
        return new ResponseEntity<>(toResource(chain), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete an IngestProcessingChain by name.")
    @DeleteMapping(value = NAME_PATH)
    public ResponseEntity<Void> delete(@PathVariable("name") String name) throws ModuleException {
        ingestProcessingService.deleteChain(name);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Create a new ingestion processing chain", role = DefaultRole.ADMIN)
    @PostMapping()
    public ResponseEntity<EntityModel<IngestProcessingChain>> create(@Valid @RequestBody
                                                                     IngestProcessingChain processingChain)
        throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.createNewChain(processingChain);
        return new ResponseEntity<>(toResource(chain), HttpStatus.CREATED);

    }

    @ResourceAccess(description = "Create a new ingestion processing chain importing JSON file",
                    role = DefaultRole.ADMIN)
    @PostMapping(value = IMPORT_PATH, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityModel<IngestProcessingChain>> createByFile(@RequestParam("file") MultipartFile file)
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

    @ResourceAccess(description = "Export an ingestion processing chain in JSON format", role = DefaultRole.ADMIN)
    @GetMapping(value = EXPORT_PATH)
    public void export(HttpServletRequest pRequest, HttpServletResponse pResponse, @PathVariable("name") String name)
        throws ModuleException {
        IngestProcessingChain chain = ingestProcessingService.getChain(name);
        String exportedFilename = applicationName + "-" + chain.getName() + ".json";

        // Produce octet stream to force navigator opening "save as" dialog
        pResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
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

    @ResourceAccess(description = "Update an existing IngestProcessingChain.", role = DefaultRole.ADMIN)
    @PutMapping(value = NAME_PATH)
    public ResponseEntity<EntityModel<IngestProcessingChain>> update(@PathVariable("name") String name,
                                                                     @Valid @RequestBody
                                                                     IngestProcessingChain processingChain)
        throws ModuleException {
        if (!name.equals(processingChain.getName())) {
            throw new EntityInvalidException("Name of entity to update does not match.");
        }
        IngestProcessingChain chain = ingestProcessingService.updateChain(processingChain);
        return new ResponseEntity<>(toResource(chain), HttpStatus.OK);
    }

    @Override
    public EntityModel<IngestProcessingChain> toResource(IngestProcessingChain ingestChain, Object... pExtras) {
        final EntityModel<IngestProcessingChain> resource = resourceService.toResource(ingestChain);
        resourceService.addLink(resource,
                                this.getClass(),
                                "get",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        if (!ingestChain.getName().equals(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "delete",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(String.class, ingestChain.getName()));
        }
        resourceService.addLink(resource,
                                this.getClass(),
                                "update",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, ingestChain.getName()),
                                MethodParamFactory.build(IngestProcessingChain.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "export",
                                LinkRelation.of("export"),
                                MethodParamFactory.build(HttpServletRequest.class),
                                MethodParamFactory.build(HttpServletResponse.class),
                                MethodParamFactory.build(String.class, ingestChain.getName()));
        return resource;
    }

}
