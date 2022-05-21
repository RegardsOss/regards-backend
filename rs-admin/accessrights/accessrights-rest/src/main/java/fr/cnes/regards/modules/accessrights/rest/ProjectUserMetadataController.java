/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Controller responsible for the /users(/*)? endpoints
 *
 * @author svissier
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(ProjectUserMetadataController.REQUEST_MAPPING_ROOT)
public class ProjectUserMetadataController implements IResourceController<MetaData> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/users/{user_id}/metadatas";

    /**
     * Service handling project users
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param userId The {@link ProjectUser}'s <code>id</code>
     * @return a{@link List} of {@link MetaData}
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "retrieve the list of all metadata of the user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<EntityModel<MetaData>>> retrieveProjectUserMetaData(@PathVariable("user_id") Long userId)
        throws EntityNotFoundException {
        final List<MetaData> metaDatas = projectUserService.retrieveUserMetaData(userId);
        return new ResponseEntity<>(toResources(metaDatas), HttpStatus.OK);
    }

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param userId              The {@link ProjectUser}'s <code>id</code>
     * @param updatedUserMetaData The {@link List} of {@link MetaData} to set
     * @return void
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "update the list of all metadata of the user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<EntityModel<MetaData>>> updateProjectUserMetaData(@PathVariable("user_id") Long userId,
                                                                                 @Valid @RequestBody
                                                                                 List<MetaData> updatedUserMetaData)
        throws EntityNotFoundException {
        List<MetaData> updated = projectUserService.updateUserMetaData(userId, updatedUserMetaData);
        return new ResponseEntity<>(toResources(updated), HttpStatus.OK);
    }

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     *
     * @param userId The {@link ProjectUser} <code>id</code>
     * @return void
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove all the metadata of the user", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUserMetaData(@PathVariable("user_id") Long userId)
        throws EntityNotFoundException {
        projectUserService.removeUserMetaData(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<MetaData> toResource(final MetaData element, final Object... extras) {
        return resourceService.toResource(element);
    }

}
