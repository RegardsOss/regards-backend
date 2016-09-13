package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import fr.cnes.regards.microservices.core.information.ModuleInfo;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.IProjectUserService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/accesses")
public class AccessesController {

    @Autowired
    private MethodAutorizationService authService;

    @Autowired
    private IProjectUserService projectUserService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        // admin can do everything!
        authService.setAutorities("/accesses@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accesses@POST", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accesses/{access_id}@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accesses/{access_id}@PUT", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accesses/{access_id}@DELETE", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accesses/settings@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accesses/settings@PUT", new RoleAuthority("ADMIN"));
        // users can just get!
        authService.setAutorities("/accesses@GET", new RoleAuthority("USER"));
    }

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

    @ResourceAccess
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ProjectUser>> retrieveAccessRequestList() {
        List<ProjectUser> projectUsers = this.projectUserService_.retrieveAccessRequestList();
        return new ResponseEntity<>(projectUsers, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<ProjectUser> requestAccess(@Valid @RequestBody ProjectUser pAccessRequest)
            throws AlreadyExistingException {
        ProjectUser created = this.projectUserService_.requestAccess(pAccessRequest);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @ResourceAccess
    @RequestMapping(value = "/{access_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<ProjectUser> retrieveAccessRequest(@PathVariable("access_id") String pAccessId) {
        ProjectUser wanted = this.projectUserService_.retrieveAccessRequest(pAccessId);
        return new ResponseEntity<>(wanted, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/{access_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateAccessRequest(@PathVariable("access_id") String pAccessId,
            @RequestBody ProjectUser pUpdatedAccessRequest) {
        this.projectUserService_.updateAccessRequest(pAccessId, pUpdatedAccessRequest);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeAccessRequest(@PathVariable("access_id") String pAccessId) {
        this.projectUserService_.retrieveAccessRequest(pAccessId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
