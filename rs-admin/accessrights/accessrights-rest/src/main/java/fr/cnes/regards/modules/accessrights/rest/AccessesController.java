/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessRequestService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.signature.IAccessesSignature;

@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class AccessesController implements IAccessesSignature {

    /**
     * Service handling CRUD operation on access requests. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessRequestService accessRequestService;

    /**
     * Service handling CRUD operation on {@link AccountSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessSettingsService accessSettingsService;

    @Override
    @ResourceAccess(description = "retrieve the list of access request", name = "")
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        final List<ProjectUser> projectUsers = accessRequestService.retrieveAccessRequestList();
        final List<Resource<ProjectUser>> resources = projectUsers.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "create a new access request", name = "")
    public ResponseEntity<Resource<AccessRequestDTO>> requestAccess(
            @Valid @RequestBody final AccessRequestDTO pAccessRequest)
            throws AlreadyExistingException, InvalidEntityException {
        final AccessRequestDTO created = accessRequestService.requestAccess(pAccessRequest);
        final Resource<AccessRequestDTO> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "accept the access request", name = "")
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws ModuleEntityNotFoundException {
        accessRequestService.acceptAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "deny the access request", name = "")
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws ModuleEntityNotFoundException {
        accessRequestService.denyAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove the access request", name = "")
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws ModuleEntityNotFoundException {
        accessRequestService.removeAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "retrieve the settings managing the access requests", name = "")
    public ResponseEntity<Resource<AccessSettings>> getAccessSettings() {
        final AccessSettings accessSettings = accessSettingsService.retrieve();
        final Resource<AccessSettings> resource = new Resource<>(accessSettings);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the setting managing the access requests", name = "")
    public ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody final AccessSettings pAccessSettings)
            throws ModuleEntityNotFoundException {
        accessSettingsService.update(pAccessSettings);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
