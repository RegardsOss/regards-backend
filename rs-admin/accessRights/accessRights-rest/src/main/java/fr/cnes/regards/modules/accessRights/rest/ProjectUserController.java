/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.NoSuchElementException;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
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
public class ProjectUserController {

    @Autowired
    private IUserService userService_;

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

    @ResourceAccess(description = "retrieve the list of users of the project", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ProjectUser>> retrieveProjectUserList() {
        List<ProjectUser> users = userService_.retrieveUserList();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @ResourceAccess(description = "retrieve the project user and only display public metadata")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<ProjectUser> retrieveProjectUser(@PathVariable("user_id") Long userId) {
        ProjectUser user = userService_.retrieveUser(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @ResourceAccess(description = "update the project user")
    // @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
    // produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateProjectUser(@PathVariable("user_id") Long userId,
            @RequestBody ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException {
        userService_.updateUser(userId, pUpdatedProjectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "remove the project user from the instance")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId) {
        userService_.removeUser(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "retrieve the list of all metadata of the user")
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<MetaData>> retrieveProjectUserMetaData(@PathVariable("user_id") Long pUserId) {
        return new ResponseEntity<>(userService_.retrieveUserMetaData(pUserId), HttpStatus.OK);
    }

    @ResourceAccess(description = "update the list of all metadata of the user")
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") Long userId,
            @Valid @RequestBody List<MetaData> pUpdatedUserMetaData) throws OperationNotSupportedException {
        userService_.updateUserMetaData(userId, pUpdatedUserMetaData);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "remove all the metadata of the user")
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") Long userId) {
        userService_.removeUserMetaData(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "retrieve the list of specific access rights and the role of the project user")
    @RequestMapping(value = "/{user_id}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Couple<List<ResourcesAccess>, Role>> retrieveProjectUserAccessRights(
            @PathVariable("user_id") Long pUserId,
            @RequestParam(value = "borrowedRoleName", required = false) String pBorrowedRoleName)
            throws OperationNotSupportedException {
        return new ResponseEntity<>(userService_.retrieveUserAccessRights(pUserId, pBorrowedRoleName), HttpStatus.OK);
    }

    @ResourceAccess(description = "update the list of specific user access rights")
    @RequestMapping(value = "/{user_id}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateProjectUserAccessRights(@PathVariable("user_id") Long userId,
            @Valid @RequestBody List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException {
        userService_.updateUserAccessRights(userId, pUpdatedUserAccessRights);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "remove all the specific access rights")
    @RequestMapping(value = "/{user_id}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeProjectUserAccessRights(@PathVariable("user_id") Long userId) {
        userService_.removeUserAccessRights(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
