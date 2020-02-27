/**
 *
 */
package fr.cnes.regards.modules.notifier.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.notifier.domain.Recipient;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import fr.cnes.reguards.modules.notifier.dto.RecipientDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST interface for managing data {@link Recipient}
 * @author kevin marchois
 *
 */
@RestController
@RequestMapping(RecipientController.RECIPIENT)
public class RecipientController implements IResourceController<RecipientDto> {

    public static final String RECIPIENT = "/recipient";

    public static final String ID = "/{id}";

    @Autowired
    private IRecipientService recipientService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Get all {@link Recipient} from database the result will be paginated and transformed to {@link RecipientDto}
     * @param page
     * @param assembler
     * @return paged list of {@link RecipientDto}
     */
    @ResourceAccess(description = "List all recipient")
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "List all recipient", description = "List all recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Recipients") })
    public ResponseEntity<PagedModel<EntityModel<RecipientDto>>> getRecipients(
            @Parameter(description = "Request page") Pageable page,
            final PagedResourcesAssembler<RecipientDto> assembler) {
        return ResponseEntity.ok(toPagedResources(this.recipientService.getRecipients(page), assembler));
    }

    /**
     * Create a {@link Recipient}
     * @return the created {@link Recipient}
     */
    @ResourceAccess(description = "Create a recipient")
    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create a recipient", description = "Create a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Created Recipient") })
    public ResponseEntity<EntityModel<RecipientDto>> createRecipient(
            @Parameter(description = "Recipient to create") @Valid @RequestBody RecipientDto toCreate) {
        return ResponseEntity.ok(toResource(this.recipientService.createOrUpdateRecipient(toCreate)));
    }

    /**
     * Update a {@link Recipient}
     * @return the updated {@link Recipient}
     */
    @ResourceAccess(description = "Update a recipient")
    @RequestMapping(method = RequestMethod.PUT)
    @Operation(summary = "Update a recipient", description = "Update a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Updated Recipient") })
    public ResponseEntity<EntityModel<RecipientDto>> updateRecipient(
            @Parameter(description = "Recipient to update") @Valid @RequestBody RecipientDto toUpdate) {
        return ResponseEntity.ok(toResource(this.recipientService.createOrUpdateRecipient(toUpdate)));
    }

    /**
     * Delete a {@link Recipient}
     */
    @ResourceAccess(description = "Delete a recipient")
    @RequestMapping(method = RequestMethod.DELETE)
    @Operation(summary = "Delete a recipient", description = "Delete a recipient")
    @ApiResponses(value = { @ApiResponse(responseCode = "200") })
    public ResponseEntity<Void> deleteRecipient(
            @Parameter(description = "Recipient to delete id") @PathVariable("id") Long id) {
        this.recipientService.deleteRecipient(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public EntityModel<RecipientDto> toResource(RecipientDto element, Object... extras) {

        EntityModel<RecipientDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "getRecipients", LinkRels.SELF,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "createRecipient", LinkRels.CREATE,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "updateRecipient", LinkRels.UPDATE,
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "deleteRecipient", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }
}
