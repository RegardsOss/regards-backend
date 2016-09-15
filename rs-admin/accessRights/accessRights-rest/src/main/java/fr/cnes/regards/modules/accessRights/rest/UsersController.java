/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
import fr.cnes.regards.modules.accessRights.service.IAccessRequestService;
import fr.cnes.regards.modules.accessRights.service.IUserService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 *
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 *
 */
@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private MethodAutorizationService authService;

    @Autowired
    private IAccessRequestService projectUserService_;

    @Autowired
    private IUserService userService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        // admin can do everything!
        authService.setAutorities("/users@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users@POST", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}/accept@PUT", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}/deny@PUT", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}@DELETE", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}/permissions@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}/permissions@PUT", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}/permissions@DELETE", new RoleAuthority("ADMIN"));
        // users can just get!
        authService.setAutorities("/users@GET", new RoleAuthority("USER"));
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

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void illegalState() {
    }

    @ResourceAccess(description = "retrieve the list of users of the project")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ProjectUser>> retrieveProjectUserList() {
        List<ProjectUser> users = this.userService_.retrieveUserList();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

}
