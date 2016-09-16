/*
 * LICENSE_PLACEHOLDER
 */
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
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
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
    private IUserService userService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        // admin can do everything!
        authService.setAutorities("/users@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users@POST", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/users/{user_id}@PUT", new RoleAuthority("ADMIN"));
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
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "operation not supported")
    public void operationNotSupported() {
    }

    @ExceptionHandler(InvalidValueException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "invalid Value")
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

    @ResourceAccess(description = "")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<ProjectUser> retrieveProjectUser(@PathVariable("user_id") int userId) {
        ProjectUser user = this.userService_.retrieveUser(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT, consumes= MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateProjectUser(@PathVariable("user_id") int userId,
            @Valid @RequestBody ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException {
        this.userService_.updateUser(userId, pUpdatedProjectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeProjectUser(@PathVariable("user_id") int userId) {
        this.userService_.removeUser(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/{user_id}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Couple<List<ResourcesAccess>, Role>> retrieveProjectUserAccessRights(
            @PathVariable("user_id") int pUserId) {
        return new ResponseEntity<>(this.userService_.retrieveUserAccessRights(pUserId), HttpStatus.OK);
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/{user_id}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateProjectUserAccessRights(@PathVariable("user_id") int userId,
            @Valid @RequestBody List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException {
        this.userService_.updateUserAccessRights(userId, pUpdatedUserAccessRights);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/{user_id}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeProjectUserAccessRights(@PathVariable("user_id") int userId) {
        this.userService_.removeUserAccessRights(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
