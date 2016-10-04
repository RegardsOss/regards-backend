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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.accessRights.domain.Couple;
import fr.cnes.regards.modules.accessRights.domain.MetaData;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

@RequestMapping("/users")
public interface ProjectUsersSignature {

    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveProjectUserList();

    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long userId);

    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUser(@PathVariable("user_id") Long userId,
            @RequestBody ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException;

    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId);

    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(@PathVariable("user_id") Long pUserId);

    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") Long userId,
            @Valid @RequestBody List<MetaData> pUpdatedUserMetaData) throws OperationNotSupportedException;

    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") Long userId);

    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Couple<List<ResourcesAccess>, Role>>> retrieveProjectUserAccessRights(
            @PathVariable("user_login") String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) String pBorrowedRoleName)
            throws OperationNotSupportedException;

    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUserAccessRights(@PathVariable("user_login") String pUserLogin,
            @Valid @RequestBody List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException;

    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUserAccessRights(@PathVariable("user_login") String pUserLogin);
}
