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
package fr.cnes.regards.modules.notifier.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import fr.cnes.regards.modules.notifier.service.IRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * REST interface for managing data {@link PluginConfiguration}(recipient)
 *
 * @author kevin marchois
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(RecipientController.RECIPIENT_ROOT_PATH)
public class RecipientController implements IResourceController<PluginConfiguration> {

    public static final String RECIPIENT_ROOT_PATH = "/recipient";

    public static final String ID_PATH = "/{id}";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipientController.class);

    @Autowired
    private IRecipientService recipientService;

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IRuleService ruleService;

    /**
     * Get all {@link PluginConfiguration}(recipient) from database the result will be paginated
     *
     * @return paged list of {@link PluginConfiguration}(recipient)
     */
    // TODO : not use ??
    @Deprecated
    //the path of GET url is not correct, missing S : see RecipientDtoController in order to retrieve all recipients (path:/recipients)
    @ResourceAccess(description = "List all recipient")
    @GetMapping
    @Operation(summary = "List all recipient", description = "List all recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All recipients") })
    public ResponseEntity<List<EntityModel<PluginConfiguration>>> getRecipients(
        @Parameter(description = "Request page") Pageable page,
        @Parameter(hidden = true) final PagedResourcesAssembler<PluginConfiguration> assembler) {
        return ResponseEntity.ok(toResources(recipientService.getRecipients()));
    }

    /**
     * Create a {@link PluginConfiguration}(recipient)
     *
     * @return the created {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "Create a recipient")
    @PostMapping
    @Operation(summary = "Create a recipient", description = "Create a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Created Recipient") })
    public ResponseEntity<EntityModel<PluginConfiguration>> createRecipient(
        @Parameter(description = "Recipient to create") @Valid @RequestBody PluginConfiguration toCreate) {
        Assert.isNull(toCreate.getId(), "Its a creation id must be null!");
        try {
            return ResponseEntity.ok(toResource(recipientService.createOrUpdate(toCreate)));
        } catch (ModuleException e) {
            LOGGER.error("Failed to create a recipient", e);
            return null;
        }
    }

    /**
     * Update a {@link PluginConfiguration}(recipient)
     *
     * @return the updated {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "Update a recipient")
    @PutMapping
    @Operation(summary = "Update a recipient", description = "Update a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Updated Recipient") })
    public ResponseEntity<EntityModel<PluginConfiguration>> updateRecipient(
        @Parameter(description = "Recipient to update") @Valid @RequestBody PluginConfiguration toUpdate)
        throws ModuleException {
        Assert.notNull(toUpdate.getId(), "Its a validation id must not be null!");
        PluginConfiguration updatedRecipient = recipientService.createOrUpdate(toUpdate);
        ruleService.recipientUpdated(updatedRecipient.getBusinessId());
        return ResponseEntity.ok(toResource(updatedRecipient));
    }

    /**
     * Delete a {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "Delete a recipient")
    @DeleteMapping(path = ID_PATH)
    @Operation(summary = "Delete a recipient", description = "Delete a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200") })
    public ResponseEntity<Void> deleteRecipient(
        @Parameter(description = "Recipient to delete id") @PathVariable("businessId") String businessId)
        throws ModuleException {
        ruleService.recipientDeleted(businessId);
        recipientService.delete(businessId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public EntityModel<PluginConfiguration> toResource(PluginConfiguration element, Object... extras) {
        EntityModel<PluginConfiguration> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "getRecipients",
                                LinkRels.SELF,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "createRecipient",
                                LinkRels.CREATE,
                                MethodParamFactory.build(PluginConfiguration.class, element));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateRecipient",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(PluginConfiguration.class, element));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteRecipient",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, element.getBusinessId()));
        return resource;
    }
}