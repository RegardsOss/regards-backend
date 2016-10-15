/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessRights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.IAccessRequestService;
import fr.cnes.regards.modules.accessRights.signature.IAccessesSignature;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class AccessesController implements IAccessesSignature {

    @Autowired
    private IAccessRequestService accessRequestService;

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void dataNotFound() {
    }

    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void operationNotSupported() {
    }

    @ExceptionHandler(InvalidValueException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void invalidValue() {
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void illegalState() {
    }

    @Override
    @ResourceAccess(description = "retrieve the list of access request", name = "")
    public HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        final List<ProjectUser> projectUsers = accessRequestService.retrieveAccessRequestList();
        final List<Resource<ProjectUser>> resources = projectUsers.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "create a new access request", name = "")
    public HttpEntity<Resource<AccessRequestDTO>> requestAccess(
            @Valid @RequestBody final AccessRequestDTO pAccessRequest) throws AlreadyExistingException {
        final AccessRequestDTO created = accessRequestService.requestAccess(pAccessRequest);
        final Resource<AccessRequestDTO> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "accept the access request", name = "")
    public HttpEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws OperationNotSupportedException {
        accessRequestService.acceptAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "deny the access request", name = "")
    public HttpEntity<Void> denyAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws OperationNotSupportedException {
        accessRequestService.denyAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove the access request", name = "")
    public HttpEntity<Void> removeAccessRequest(@PathVariable("access_id") final Long pAccessId) {
        accessRequestService.removeAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "retrieve the list of setting managing the access requests", name = "")
    public HttpEntity<List<Resource<String>>> getAccessSettingList() {
        final List<String> accessSettings = accessRequestService.getAccessSettingList();
        final List<Resource<String>> resources = accessSettings.stream().map(a -> new Resource<>(a))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the setting managing the access requests", name = "")
    public HttpEntity<Void> updateAccessSetting(@Valid @RequestBody final String pUpdatedProjectUserSetting)
            throws InvalidValueException {
        accessRequestService.updateAccessSetting(pUpdatedProjectUserSetting);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
