/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.signature.IProjectUsersSignature;

/**
 *
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 *
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
public class ProjectUsersController implements IProjectUsersSignature {

    @Autowired
    private IProjectUserService projectUserService;

    @Override
    @ResourceAccess(description = "retrieve the list of users of the project")
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveProjectUserList() {
        final List<ProjectUser> users = projectUserService.retrieveUserList();
        final List<Resource<ProjectUser>> resources = users.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "retrieve the project user and only display  metadata")
    public ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_email") final String userEmail)
            throws ModuleEntityNotFoundException {
        final ProjectUser user = projectUserService.retrieveOneByEmail(userEmail);
        final Resource<ProjectUser> resource = new Resource<>(user);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the project user")
    public ResponseEntity<Void> updateProjectUser(@PathVariable("user_id") final Long userId,
            @RequestBody final ProjectUser pUpdatedProjectUser)
            throws InvalidValueException, ModuleEntityNotFoundException {
        projectUserService.updateUser(userId, pUpdatedProjectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove the project user from the instance")
    public ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") final Long userId) {
        projectUserService.removeUser(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "retrieve the list of all metadata of the user")
    public ResponseEntity<List<Resource<MetaData>>> retrieveProjectUserMetaData(
            @PathVariable("user_id") final Long pUserId) throws ModuleEntityNotFoundException {
        final List<MetaData> metaDatas = projectUserService.retrieveUserMetaData(pUserId);
        final List<Resource<MetaData>> resources = metaDatas.stream().map(m -> new Resource<>(m))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the list of all metadata of the user")
    public ResponseEntity<Void> updateProjectUserMetaData(@PathVariable("user_id") final Long userId,
            @Valid @RequestBody final List<MetaData> pUpdatedUserMetaData) throws ModuleEntityNotFoundException {
        projectUserService.updateUserMetaData(userId, pUpdatedUserMetaData);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove all the metadata of the user")
    public ResponseEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") final Long userId)
            throws ModuleEntityNotFoundException {
        projectUserService.removeUserMetaData(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "retrieve the list of specific access rights and the role of the project user")
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserAccessRights(
            @PathVariable("user_login") final String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) final String pBorrowedRoleName)
            throws InvalidValueException, EntityNotFoundException {
        final List<ResourcesAccess> permissions = projectUserService.retrieveProjectUserAccessRights(pUserLogin,
                                                                                                     pBorrowedRoleName);

        final List<Resource<ResourcesAccess>> result = new ArrayList<>();
        for (final ResourcesAccess item : permissions) {
            result.add(new Resource<>(item));
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the list of specific user access rights")
    public ResponseEntity<Void> updateProjectUserAccessRights(@PathVariable("user_login") final String pLogin,
            @Valid @RequestBody final List<ResourcesAccess> pUpdatedUserAccessRights)
            throws ModuleEntityNotFoundException {
        projectUserService.updateUserAccessRights(pLogin, pUpdatedUserAccessRights);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResponseBody
    @ResourceAccess(description = "remove all the specific access rights")
    public ResponseEntity<Void> removeProjectUserAccessRights(@PathVariable("user_login") final String pUserLogin)
            throws ModuleEntityNotFoundException {
        projectUserService.removeUserAccessRights(pUserLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
