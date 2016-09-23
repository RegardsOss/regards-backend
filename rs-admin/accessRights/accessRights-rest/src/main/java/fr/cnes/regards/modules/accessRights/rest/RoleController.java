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

import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessRights.domain.HateoasDTO;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/roles")
public class RoleController {

    // @Autowired
    // private MethodAutorizationService authService_;

    @Autowired
    private IRoleService roleService_;

    /**
     * Method to initate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        // admin can do everything!
        // authService_.setAutorities("/roles@GET", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles@POST", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}@GET", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}@PUT", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}@DELETE", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}/permissions@GET", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}/permissions@PUT", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}/permissions@DELETE", new RoleAuthority("ADMIN"));
        // authService_.setAutorities("/roles/{role_id}/users@GET", new RoleAuthority("ADMIN"));
        // // users can just get!
        // authService_.setAutorities("/roles@GET", new RoleAuthority("ADMIN"));
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

    @ResourceAccess(description = "Retrieve the list of roles", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<HateoasDTO<List<Role>>> retrieveRoleList() {
        List<Role> roles = roleService_.retrieveRoleList();
        HateoasDTO<List<Role>> resource = new HateoasDTO<>(roles);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @ResourceAccess(description = "Create a role", name = "")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<HateoasDTO<Role>> createRole(@Valid @RequestBody Role pNewRole)
            throws AlreadyExistingException {
        Role created = roleService_.createRole(pNewRole);
        HateoasDTO<Role> resource = new HateoasDTO<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @ResourceAccess(description = "Retrieve a role by id", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Role> retrieveRole(@PathVariable("role_id") Long pRoleId) {
        Role role = roleService_.retrieveRole(pRoleId);
        return new ResponseEntity<>(role, HttpStatus.OK);
    }

    @ResourceAccess(description = "Update the role of role_id with passed body", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateRole(@PathVariable("role_id") Long pRoleId,
            @Valid @RequestBody Role pUpdatedRole) throws OperationNotSupportedException {
        roleService_.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Remove the role of role_id", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeRole(@PathVariable("role_id") Long pRoleId)
            throws OperationNotSupportedException {
        roleService_.removeRole(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ResourcesAccess>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") Long pRoleId) {
        List<ResourcesAccess> resourcesAccesses = roleService_.retrieveRoleResourcesAccessList(pRoleId);
        return new ResponseEntity<>(resourcesAccesses, HttpStatus.OK);
    }

    @ResourceAccess(description = "Incrementally update the list of permissions of the role with role_id", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") Long pRoleId,
            @Valid @RequestBody List<ResourcesAccess> pResourcesAccessList) throws OperationNotSupportedException {
        roleService_.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Clear the list of permissions of the", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") Long pRoleId)
            throws OperationNotSupportedException {
        roleService_.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve the list of project users of the role with role_id", name = "")
    @RequestMapping(value = "/{role_id}/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ProjectUser>> retrieveRoleProjectUserList(
            @PathVariable("role_id") Long pRoleId) {
        List<ProjectUser> projectUserList = roleService_.retrieveRoleProjectUserList(pRoleId);
        return new ResponseEntity<>(projectUserList, HttpStatus.OK);
    }

}