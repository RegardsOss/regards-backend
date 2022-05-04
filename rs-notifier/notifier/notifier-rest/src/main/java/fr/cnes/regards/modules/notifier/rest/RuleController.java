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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.dto.RuleDTO;
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
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST interface for managing data {@link Rule}
 *
 * @author kevin marchois
 */
@RestController
@RequestMapping(RuleController.RULE)
public class RuleController implements IResourceController<RuleDTO> {

    public static final String RULE = "/rule";

    public static final String ID = "/{id}";

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleController.class);

    @Autowired
    private IRuleService ruleService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link Rule} from database the result will be paginated and transformed to {@link RuleDTO}
     *
     * @return paged list of {@link RuleDTO}
     */
    @ResourceAccess(description = "List all Rules")
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "List all rules", description = "List all Rules")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All Rules") })
    public ResponseEntity<PagedModel<EntityModel<RuleDTO>>> getRules(
        @Parameter(description = "Wanted page") Pageable page, final PagedResourcesAssembler<RuleDTO> assembler) {
        return ResponseEntity.ok(toPagedResources(this.ruleService.getRules(page), assembler));
    }

    /**
     * Create a {@link Rule}
     *
     * @return the created {@link Rule}
     */
    @ResourceAccess(description = "Create a Rule")
    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create a rule", description = "Create a Rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Created Rule") })
    public ResponseEntity<EntityModel<RuleDTO>> createRule(
        @Parameter(description = "Rule to create") @Valid @RequestBody RuleDTO toCreate) {
        Assert.isNull(toCreate.getId(), "Its a creation id must be null!");
        try {
            return ResponseEntity.ok(toResource(this.ruleService.createOrUpdate(toCreate)));
        } catch (ModuleException e) {
            LOGGER.error("Impossible! how can it throwed for a creation", e);
            return null;
        }
    }

    /**
     * Update a {@link Rule}
     *
     * @return the updated {@link Rule}
     */
    @ResourceAccess(description = "Update a Rule")
    @RequestMapping(method = RequestMethod.PUT)
    @Operation(summary = "Update a rule", description = "Update a Rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Updated Rule") })
    public ResponseEntity<EntityModel<RuleDTO>> updateRule(
        @Parameter(description = "Rule to update") @Valid @RequestBody RuleDTO toUpdate) throws ModuleException {
        Assert.notNull(toUpdate.getId(), "Its a validation id must not be null!");

        return ResponseEntity.ok(toResource(this.ruleService.createOrUpdate(toUpdate)));
    }

    /**
     * Delete a {@link Rule}
     */
    @ResourceAccess(description = "Delete a rule")
    @RequestMapping(path = ID, method = RequestMethod.DELETE)
    @Operation(summary = "Delete a rule", description = "Delete a rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200") })
    public ResponseEntity<Void> deleteRule(@PathVariable("id") String id) throws ModuleException {
        this.ruleService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public EntityModel<RuleDTO> toResource(RuleDTO element, Object... extras) {
        EntityModel<RuleDTO> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "getRules",
                                LinkRels.SELF,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "createRule",
                                LinkRels.CREATE,
                                MethodParamFactory.build(RuleDTO.class, element));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateRule",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(RuleDTO.class, element));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteRule",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, element.getId()));
        return resource;
    }
}
