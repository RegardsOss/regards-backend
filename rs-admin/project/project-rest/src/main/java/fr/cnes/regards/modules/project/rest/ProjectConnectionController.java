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
package fr.cnes.regards.modules.project.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Project connection API
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(ProjectConnectionController.TYPE_MAPPING)
public class ProjectConnectionController implements IResourceController<ProjectConnection> {

    /**
     * Type mapping
     */
    public static final String TYPE_MAPPING = "/projects/{projectName}/connections";

    /**
     * Resource id mapping
     */
    public static final String RESOURCE_ID_MAPPING = "/{connectionId}";

    /**
     * {@link ProjectConnection} service
     */
    private final IProjectConnectionService projectConnectionService;

    /**
     * Resource service
     */
    private final IResourceService resourceService;

    public ProjectConnectionController(IProjectConnectionService projectConnectionService,
                                       IResourceService resourceService) {
        this.projectConnectionService = projectConnectionService;
        this.resourceService = resourceService;
    }

    /**
     * Retrieve all project connections
     *
     * @param projectName project name (i.e. tenant)
     * @param pageable    pageable
     * @param assembler   assembler
     * @return all project connections
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all projects connections for a given project/tenant",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<ProjectConnection>>> getAllProjectConnections(
        @PathVariable String projectName,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ProjectConnection> assembler) {
        Page<ProjectConnection> connections = projectConnectionService.retrieveProjectsConnectionsByProject(projectName,
                                                                                                            pageable);
        return ResponseEntity.ok(toPagedResources(connections, assembler));
    }

    /**
     * Retrieve a single project connection by identifier
     *
     * @param projectName  project name
     * @param connectionId connection identifier
     * @return a project connection
     * @throws ModuleException if error occurs
     */
    @RequestMapping(method = RequestMethod.GET, value = ProjectConnectionController.RESOURCE_ID_MAPPING)
    @ResourceAccess(description = "Retrieve a project connection of a given project/tenant",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<ProjectConnection>> getProjectConnection(@PathVariable String projectName,
                                                                               @PathVariable Long connectionId)
        throws ModuleException {
        ProjectConnection pConn = projectConnectionService.retrieveProjectConnectionById(connectionId);
        return ResponseEntity.ok(toResource(pConn));
    }

    /**
     * Create a new project connection
     *
     * @param projectName       project name
     * @param projectConnection connection to create
     * @return the create project connection
     * @throws ModuleException if error occurs
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a new project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<ProjectConnection>> createProjectConnection(@PathVariable String projectName,
                                                                                  @Valid @RequestBody
                                                                                  ProjectConnection projectConnection)
        throws ModuleException {
        ProjectConnection connection = projectConnectionService.createProjectConnection(projectConnection, false);
        return ResponseEntity.ok(toResource(connection));
    }

    /**
     * Update an existing project connection
     *
     * @param projectName       project name
     * @param connectionId      project connection identifier
     * @param projectConnection project connection
     * @return updated connection
     * @throws ModuleException if error occurs
     */
    @RequestMapping(method = RequestMethod.PUT, value = ProjectConnectionController.RESOURCE_ID_MAPPING)
    @ResourceAccess(description = "Update a project connection", role = DefaultRole.INSTANCE_ADMIN)

    public ResponseEntity<EntityModel<ProjectConnection>> updateProjectConnection(@PathVariable String projectName,
                                                                                  @PathVariable Long connectionId,
                                                                                  @Valid @RequestBody
                                                                                  ProjectConnection projectConnection)
        throws ModuleException {
        ProjectConnection connection = projectConnectionService.updateProjectConnection(connectionId,
                                                                                        projectConnection);
        return ResponseEntity.ok(toResource(connection));
    }

    /**
     * Delete an existing project connection
     *
     * @param projectName  project name
     * @param connectionId project connection identifier
     * @return {@link Void}
     * @throws ModuleException if error occurs
     */
    @RequestMapping(method = RequestMethod.DELETE, value = ProjectConnectionController.RESOURCE_ID_MAPPING)
    @ResourceAccess(description = "delete a project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> deleteProjectConnection(@PathVariable String projectName,
                                                        @PathVariable Long connectionId) throws ModuleException {
        projectConnectionService.deleteProjectConnection(connectionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public EntityModel<ProjectConnection> toResource(ProjectConnection element, Object... extras) {
        final EntityModel<ProjectConnection> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "getProjectConnection",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getProject().getName()),
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateProjectConnection",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, element.getProject().getName()),
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(ProjectConnection.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteProjectConnection",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, element.getProject().getName()),
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "getAllProjectConnections",
                                LinkRels.LIST,
                                MethodParamFactory.build(String.class, element.getProject().getName()),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        return resource;
    }
}
