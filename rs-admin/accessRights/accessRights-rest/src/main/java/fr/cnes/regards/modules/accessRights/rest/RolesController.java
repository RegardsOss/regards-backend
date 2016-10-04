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

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.signature.RolesSignature;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
public class RolesController implements RolesSignature {

    @Autowired
    private IRoleService roleService_;

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

    @Override
    @ResourceAccess(description = "Retrieve the list of roles", name = "")
    public HttpEntity<List<Resource<Role>>> retrieveRoleList() {
        List<Role> roles = roleService_.retrieveRoleList();
        List<Resource<Role>> resources = roles.stream().map(r -> new Resource<>(r)).collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Create a role", name = "")
    public HttpEntity<Resource<Role>> createRole(@Valid @RequestBody Role pNewRole) throws AlreadyExistingException {
        Role created = roleService_.createRole(pNewRole);
        Resource<Role> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "Retrieve a role by id", name = "")
    public HttpEntity<Resource<Role>> retrieveRole(@PathVariable("role_id") Long pRoleId) {
        Role role = roleService_.retrieveRole(pRoleId);
        Resource<Role> resource = new Resource<>(role);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Update the role of role_id with passed body", name = "")
    public HttpEntity<Void> updateRole(@PathVariable("role_id") Long pRoleId, @Valid @RequestBody Role pUpdatedRole)
            throws OperationNotSupportedException {
        roleService_.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Remove the role of role_id", name = "")
    public HttpEntity<Void> removeRole(@PathVariable("role_id") Long pRoleId) throws OperationNotSupportedException {
        roleService_.removeRole(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id", name = "")
    public HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") Long pRoleId) {
        List<ResourcesAccess> resourcesAccesses = roleService_.retrieveRoleResourcesAccessList(pRoleId);
        List<Resource<ResourcesAccess>> resources = resourcesAccesses.stream().map(ra -> new Resource<>(ra))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Incrementally update the list of permissions of the role with role_id", name = "")
    public HttpEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") Long pRoleId,
            @Valid @RequestBody List<ResourcesAccess> pResourcesAccessList) throws OperationNotSupportedException {
        roleService_.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Clear the list of permissions of the", name = "")
    public HttpEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") Long pRoleId)
            throws OperationNotSupportedException {
        roleService_.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Retrieve the list of project users of the role with role_id", name = "")
    public HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(@PathVariable("role_id") Long pRoleId)
            throws OperationNotSupportedException {
        List<ProjectUser> projectUserList = roleService_.retrieveRoleProjectUserList(pRoleId);
        List<Resource<ProjectUser>> resources = projectUserList.stream().map(pu -> new Resource<>(pu))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

}