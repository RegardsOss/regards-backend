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
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

public interface ProjectUsersSignature {

    @ResourceAccess(description = "retrieve the list of users of the project", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveProjectUserList();

    @ResourceAccess(description = "retrieve the project user and only display  metadata")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long userId);

    @ResourceAccess(description = "update the project user")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUser(@PathVariable("user_id") Long userId,
            @RequestBody ProjectUser pUpdatedProjectUser) throws OperationNotSupportedException;

    @ResourceAccess(description = "remove the project user from the instance")
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUser(@PathVariable("user_id") Long userId);

    @ResourceAccess(description = "retrieve the list of all metadata of the user")
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(@PathVariable("user_id") Long pUserId);

    @ResourceAccess(description = "update the list of all metadata of the user")
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") Long userId,
            @Valid @RequestBody List<MetaData> pUpdatedUserMetaData) throws OperationNotSupportedException;

    @ResourceAccess(description = "remove all the metadata of the user")
    @RequestMapping(value = "/{user_id}/metadata", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") Long userId);

    @ResourceAccess(description = "retrieve the list of specific access rights and the role of the project user")
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Couple<List<ResourcesAccess>, Role>>> retrieveProjectUserAccessRights(
            @PathVariable("user_login") String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) String pBorrowedRoleName)
            throws OperationNotSupportedException;

    @ResourceAccess(description = "update the list of specific user access rights")
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateProjectUserAccessRights(@PathVariable("user_login") String pUserLogin,
            @Valid @RequestBody List<ResourcesAccess> pUpdatedUserAccessRights) throws OperationNotSupportedException;

    @ResourceAccess(description = "remove all the specific access rights")
    @RequestMapping(value = "/{user_login}/permissions", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeProjectUserAccessRights(@PathVariable("user_login") String pUserLogin);
}
