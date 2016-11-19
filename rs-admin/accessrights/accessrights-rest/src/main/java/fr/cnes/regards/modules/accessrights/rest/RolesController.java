/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.signature.IRolesSignature;

@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class RolesController implements IRolesSignature {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesController.class);

    @Autowired
    private IRoleService roleService;

    @Override
    @ResourceAccess(description = "Retrieve the list of roles")
    public ResponseEntity<List<Resource<Role>>> retrieveRoleList() {
        final List<Role> roles = roleService.retrieveRoleList();
        final List<Resource<Role>> resources = roles.stream().map(r -> new Resource<>(r)).collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Create a role")
    public ResponseEntity<Resource<Role>> createRole(@Valid @RequestBody final Role pNewRole)
            throws AlreadyExistingException {
        final Role created = roleService.createRole(pNewRole);
        final Resource<Role> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "Retrieve a role by id")
    public ResponseEntity<Resource<Role>> retrieveRole(@PathVariable("role_name") final String pRoleName)
            throws ModuleEntityNotFoundException {
        final Role role = roleService.retrieveRole(pRoleName);
        final Resource<Role> resource = new Resource<>(role);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Update the role of role_id with passed body")
    public ResponseEntity<Void> updateRole(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final Role pUpdatedRole) throws ModuleEntityNotFoundException, InvalidValueException {
        roleService.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Remove the role of role_id")
    public ResponseEntity<Void> removeRole(@PathVariable("role_id") final Long pRoleId)
            throws OperationForbiddenException {
        roleService.removeRole(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id")
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") final Long pRoleId) throws ModuleEntityNotFoundException {
        final List<ResourcesAccess> resourcesAccesses = roleService.retrieveRoleResourcesAccessList(pRoleId);
        final List<Resource<ResourcesAccess>> resources = resourcesAccesses.stream().map(ra -> new Resource<>(ra))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Incrementally update the list of permissions of the role with role_id")
    public ResponseEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final List<ResourcesAccess> pResourcesAccessList) throws ModuleEntityNotFoundException {
        roleService.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "Clear the list of permissions of the")
    public ResponseEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId)
            throws ModuleEntityNotFoundException {
        roleService.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(
            description = "Retrieve the list of project users (crawls through parents' hierarachy) of the role with role_id")
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId) {
        List<ProjectUser> projectUserList = new ArrayList<>();
        try {
            projectUserList = roleService.retrieveRoleProjectUserList(pRoleId);
        } catch (final ModuleEntityNotFoundException e) {
            LOG.error("Unable to retrieve the project user list", e);
        }
        final List<Resource<ProjectUser>> resources = projectUserList.stream().map(pu -> new Resource<>(pu))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

}