package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import fr.cnes.regards.microservices.core.information.ModuleInfo;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.service.IRoleService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/roles")
public class RolesController {

    @Autowired
    private MethodAutorizationService authService_;

    @Autowired
    private IRoleService rolesService_;

    /**
     * Method to iniate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        // admin can do everything!
        authService_.setAutorities("/roles@GET", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles@POST", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}@GET", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}@OPUT", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}@DELETE", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}/permissions@GET", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}/permissions@PUT", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}/permissions@DELETE", new RoleAuthority("ADMIN"));
        authService_.setAutorities("/roles/{role_id}/users@GET", new RoleAuthority("ADMIN"));
        // users can just get!
        authService_.setAutorities("/roles@GET", new RoleAuthority("ADMIN"));
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<Role>> retrieveRoleList() {
        List<Role> roles = this.rolesService_.retrieveRoleList();
        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Role> createRole(@Valid @RequestBody Role pNewRole)
            throws AlreadyExistingException {
        Role created = this.rolesService_.createRole(pNewRole);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Role> retrieveRole(@PathVariable("role_id") String pRoleId) {
        Role role = this.rolesService_.retrieveRole(pRoleId);
        return new ResponseEntity<>(role, HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateRole(@PathVariable("role_id") String pRoleId,
            @Valid @RequestBody Role pUpdatedRole) throws OperationNotSupportedException {
        this.rolesService_.updateRole(pRoleId, pUpdatedRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeRole(@PathVariable("role_id") String pRoleId)
            throws OperationNotSupportedException {
        this.rolesService_.removeRole(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ResourcesAccess>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") String pRoleId) {
        List<ResourcesAccess> resourcesAccesses = this.rolesService_.retrieveRoleResourcesAccessList(pRoleId);
        return new ResponseEntity<>(resourcesAccesses, HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") String pRoleId,
            @Valid @RequestBody List<ResourcesAccess> pResourcesAccessList) throws OperationNotSupportedException {
        this.rolesService_.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") String pRoleId)
            throws OperationNotSupportedException {
        this.rolesService_.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "", name = "")
    @RequestMapping(value = "/{role_id}/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<ProjectUser>> retrieveRoleProjectUserList(
            @PathVariable("role_id") String pRoleId) {
        List<ProjectUser> projectUserList = this.rolesService_.retrieveRoleProjectUserList(pRoleId);
        return new ResponseEntity<>(projectUserList, HttpStatus.OK);
    }

}