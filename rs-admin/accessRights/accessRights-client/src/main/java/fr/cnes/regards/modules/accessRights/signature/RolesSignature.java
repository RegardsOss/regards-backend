/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.signature;

import java.util.List;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

public interface RolesSignature {

    @ResourceAccess(description = "Retrieve the list of roles", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<Role>>> retrieveRoleList();

    @ResourceAccess(description = "Create a role", name = "")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Role>> createRole(@Valid @RequestBody Role pNewRole) throws AlreadyExistingException;

    @ResourceAccess(description = "Retrieve a role by id", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Role>> retrieveRole(@PathVariable("role_id") Long pRoleId);

    @ResourceAccess(description = "Update the role of role_id with passed body", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateRole(@PathVariable("role_id") Long pRoleId, @Valid @RequestBody Role pUpdatedRole)
            throws OperationNotSupportedException;

    @ResourceAccess(description = "Remove the role of role_id", name = "")
    @RequestMapping(value = "/{role_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeRole(@PathVariable("role_id") Long pRoleId) throws OperationNotSupportedException;

    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(@PathVariable("role_id") Long pRoleId);

    @ResourceAccess(description = "Incrementally update the list of permissions of the role with role_id", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") Long pRoleId,
            @Valid @RequestBody List<ResourcesAccess> pResourcesAccessList) throws OperationNotSupportedException;

    @ResourceAccess(description = "Clear the list of permissions of the", name = "")
    @RequestMapping(value = "/{role_id}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") Long pRoleId)
            throws OperationNotSupportedException;

    @ResourceAccess(description = "Retrieve the list of project users of the role with role_id", name = "")
    @RequestMapping(value = "/{role_id}/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveRoleProjectUserList(@PathVariable("role_id") Long pRoleId)
            throws OperationNotSupportedException;
}
