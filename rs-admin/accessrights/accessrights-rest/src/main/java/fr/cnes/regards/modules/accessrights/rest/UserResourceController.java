/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 *
 * User resource management API
 *
 * @author Marc Sordi
 *
 */
@RestController
@RequestMapping(UserResourceController.TYPE_MAPPING)
public class UserResourceController implements IResourceController<ResourcesAccess> {

    /**
     * Controller base mapping
     */
    public static final String TYPE_MAPPING = "/users/{user_email}/resources";

    /**
     * Service handling project users
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Resource service to manage visible hateoas links
     */
    @Autowired
    private IResourceService hateoasService;

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the account given threw its <code>email</code>.
     *
     * @param pUserLogin
     *            The account <code>email</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return the {@link List} list of resources access
     * @throws ModuleException
     *             <br>
     *             {@link EntityOperationForbiddenException} Thrown when the passed {@link Role} is not hierarchically
     *             inferior to the true {@link ProjectUser}'s <code>role</code>.<br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserResources(
            @PathVariable("user_email") final String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) final String pBorrowedRoleName)
            throws ModuleException {
        final List<ResourcesAccess> permissions = projectUserService.retrieveProjectUserAccessRights(pUserLogin,
                                                                                                     pBorrowedRoleName);
        return new ResponseEntity<>(toResources(permissions, pUserLogin), HttpStatus.OK);
    }

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return void
     * @throws ModuleException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Update the list of specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateProjectUserResources(@PathVariable("user_email") final String pLogin,
            @Valid @RequestBody final List<ResourcesAccess> pUpdatedUserAccessRights) throws ModuleException {
        projectUserService.updateUserAccessRights(pLogin, pUpdatedUserAccessRights);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Remove all specific user accesses
     *
     * @param pUserLogin
     *            user email
     * @return {@link Void}
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @ResourceAccess(description = "Remove all specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUserResources(@PathVariable("user_email") final String pUserLogin)
            throws ModuleException {
        projectUserService.removeUserAccessRights(pUserLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<ResourcesAccess> toResource(final ResourcesAccess pElement, final Object... pExtras) {

        Assert.notNull(pExtras);
        String email = (String) pExtras[0];

        Resource<ResourcesAccess> resource = hateoasService.toResource(pElement);
        hateoasService.addLink(resource, this.getClass(), "retrieveProjectUserResources", LinkRels.SELF,
                               MethodParamFactory.build(String.class, email), MethodParamFactory.build(String.class));
        hateoasService.addLink(resource, this.getClass(), "updateProjectUserResources", LinkRels.UPDATE,
                               MethodParamFactory.build(String.class, email), MethodParamFactory.build(List.class));
        hateoasService.addLink(resource, this.getClass(), "removeProjectUserResources", LinkRels.DELETE,
                               MethodParamFactory.build(String.class, email));
        return resource;
    }
}
