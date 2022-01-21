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

import javax.validation.Valid;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST interface for managing data {@link PluginConfiguration}(recipient)
 * @author kevin marchois
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(RecipientController.RECIPIENT)
public class RecipientController implements IResourceController<PluginConfiguration> {

    public static final String RECIPIENT = "/recipient";

    public static final String ID = "/{id}";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipientController.class);

    @Autowired
    private IRecipientService recipientService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link PluginConfiguration}(recipient) from database the result will be paginated
     * @return paged list of {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "List all recipient")
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "List all recipient", description = "List all recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Recipients") })
    public ResponseEntity<List<EntityModel<PluginConfiguration>>> getRecipients(
            @Parameter(description = "Request page") Pageable page,
            final PagedResourcesAssembler<PluginConfiguration> assembler) {
        return ResponseEntity.ok(toResources(recipientService.getRecipients()));
    }

    /**
     * Create a {@link PluginConfiguration}(recipient)
     * @return the created {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "Create a recipient")
    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create a recipient", description = "Create a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Created Recipient") })
    public ResponseEntity<EntityModel<PluginConfiguration>> createRecipient(
            @Parameter(description = "Recipient to create") @Valid @RequestBody PluginConfiguration toCreate) {
        Assert.isNull(toCreate.getId(), "Its a creation id must be null!");
        try {
            return ResponseEntity.ok(toResource(recipientService.createOrUpdateRecipient(toCreate)));
        } catch (ModuleException e) {
            LOGGER.error("Impossible! how can it throwed for a creation", e);
            return null;
        }
    }

    /**
     * Update a {@link PluginConfiguration}(recipient)
     * @return the updated {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "Update a recipient")
    @RequestMapping(method = RequestMethod.PUT)
    @Operation(summary = "Update a recipient", description = "Update a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Updated Recipient") })
    public ResponseEntity<EntityModel<PluginConfiguration>> updateRecipient(
            @Parameter(description = "Recipient to update") @Valid @RequestBody PluginConfiguration toUpdate)
            throws ModuleException {
        Assert.notNull(toUpdate.getId(), "Its a validation id must not be null!");
        return ResponseEntity.ok(toResource(recipientService.createOrUpdateRecipient(toUpdate)));
    }

    /**
     * Delete a {@link PluginConfiguration}(recipient)
     */
    @ResourceAccess(description = "Delete a recipient")
    @RequestMapping(path = ID, method = RequestMethod.DELETE)
    @Operation(summary = "Delete a recipient", description = "Delete a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200") })
    public ResponseEntity<Void> deleteRecipient(
            @Parameter(description = "Recipient to delete id") @PathVariable("businessId") String businessId)
            throws ModuleException {
        recipientService.deleteRecipient(businessId);
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
