/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.accessRights.signature.IRolesSignature;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.modules.core.rest.Controller;

@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class RolesController extends Controller implements IRolesSignature {

    @Autowired
    private IRoleService roleService;

    @Override
    @ResourceAccess(description = "Retrieve the list of roles", name = "")
    public HttpEntity<List<Resource<Role>>> retrieveRoleList() {
        final List<Role> roles = roleService.retrieveRoleList();
        final List<Resource<Role>> resources = roles.stream().map(r -> new Resource<>(r)).collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Create a role", name = "")
    public HttpEntity<Resource<Role>> createRole(@Valid @RequestBody final Role pNewRole)
            throws AlreadyExistingException {
        final Role created = roleService.createRole(pNewRole);
        final Resource<Role> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "Retrieve a role by id", name = "")
    public HttpEntity<Resource<Role>> retrieveRole(@PathVariable("role_id") final Long pRoleId) {
        final Role role = roleService.retrieveRole(pRoleId);
        final Resource<Role> resource = new Resource<>(role);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Update the role of role_id with passed body", name = "")
    public HttpEntity<Void> updateRole(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final Role pUpdatedRole) throws EntityNotFoundException, InvalidValueException {
        roleService.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Remove the role of role_id", name = "")
    public HttpEntity<Void> removeRole(@PathVariable("role_id") final Long pRoleId) throws EntityNotFoundException {
        roleService.removeRole(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id", name = "")
    public HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") final Long pRoleId) throws EntityNotFoundException {
        final List<ResourcesAccess> resourcesAccesses = roleService.retrieveRoleResourcesAccessList(pRoleId);
        final List<Resource<ResourcesAccess>> resources = resourcesAccesses.stream().map(ra -> new Resource<>(ra))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Incrementally update the list of permissions of the role with role_id", name = "")
    public HttpEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final List<ResourcesAccess> pResourcesAccessList) throws EntityNotFoundException {
        roleService.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Clear the list of permissions of the", name = "")
    public HttpEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId)
            throws EntityNotFoundException {
        roleService.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Retrieve the list of project users of the role with role_id", name = "")
    public HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId) throws EntityNotFoundException {
        final List<ProjectUser> projectUserList = roleService.retrieveRoleProjectUserList(pRoleId);
        final List<Resource<ProjectUser>> resources = projectUserList.stream().map(pu -> new Resource<>(pu))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

}