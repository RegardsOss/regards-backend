/**
 *
 */
package fr.cnes.regards.modules.notifier.rest;

import javax.validation.Valid;

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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notifier.domain.Recipient;
import fr.cnes.regards.modules.notifier.domain.Rule;
import fr.cnes.regards.modules.notifier.service.IRuleService;
import fr.cnes.reguards.modules.notifier.dto.RuleDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST interface for managing data {@link Rule}
 * @author kevin marchois
 *
 */
@RestController
@RequestMapping(RuleController.RULE)
public class RuleController implements IResourceController<RuleDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipientController.class);

    public static final String RULE = "/rule";

    public static final String ID = "/{id}";

    @Autowired
    private IRuleService ruleService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link Rule} from database the result will be paginated and transformed to {@link RuleDto}
     * @param page
     * @param assembler
     * @return paged list of {@link RuleDto}
     */
    @ResourceAccess(description = "List all Rules")
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "List all rules", description = "List all Rules")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All Rules") })
    public ResponseEntity<PagedModel<EntityModel<RuleDto>>> getRules(
            @Parameter(description = "Wanted page") Pageable page, final PagedResourcesAssembler<RuleDto> assembler) {
        return ResponseEntity.ok(toPagedResources(this.ruleService.getRules(page), assembler));
    }

    /**
     * Create a {@link Rule}
     * @return the created {@link Rule}
     */
    @ResourceAccess(description = "Create a Rule")
    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create a rule", description = "Create a Rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Created Rule") })
    public ResponseEntity<EntityModel<RuleDto>> createRule(
            @Parameter(description = "Rule to create") @Valid @RequestBody RuleDto toCreate) {
        Assert.isNull(toCreate.getId(), "Its a creation id must me null!");
        try {
            return ResponseEntity.ok(toResource(this.ruleService.createOrUpdateRule(toCreate)));
        } catch (ModuleException e) {
            LOGGER.error("Impossible! how can it throwed for a creation", e);
            return null;
        }
    }

    /**
     * Update a {@link Rule}
     * @return the updated {@link Rule}
     * @throws ModuleException if unkow id
     */
    @ResourceAccess(description = "Update a Rule")
    @RequestMapping(method = RequestMethod.PUT)
    @Operation(summary = "Update a rule", description = "Update a Rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Updated Rule") })
    public ResponseEntity<EntityModel<RuleDto>> updateRule(
            @Parameter(description = "Rule to update") @Valid @RequestBody RuleDto toUpdate) throws ModuleException {
        Assert.notNull(toUpdate.getId(), "Its a validation id must not be null!");

        return ResponseEntity.ok(toResource(this.ruleService.createOrUpdateRule(toUpdate)));
    }

    /**
     * Delete a {@link Recipient}
     */
    @ResourceAccess(description = "Delete a rule")
    @RequestMapping(path = ID, method = RequestMethod.DELETE)
    @Operation(summary = "Delete a rule", description = "Delete a rule")
    @ApiResponses(value = { @ApiResponse(responseCode = "200") })
    public ResponseEntity<Void> deleteRecipient(@PathVariable("id") Long id) {
        this.ruleService.deleteRule(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public EntityModel<RuleDto> toResource(RuleDto element, Object... extras) {

        EntityModel<RuleDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "getRules", LinkRels.SELF,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "createRule", LinkRels.CREATE,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "updateRule", LinkRels.UPDATE,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "deleteRule", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }
}
