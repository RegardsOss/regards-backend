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

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Class ProjectsController
 * <p>
 * Controller for REST Access to Project entities
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping("/projects")
public class ProjectController implements IResourceController<Project> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectController.class);

    /**
     * Business service for Project entities. Autowired.
     */
    @Autowired
    private IProjectService projectService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private ILicenseClient licenseClient;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Retrieve projects list
     *
     * @return List of projects
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResourceAccess(description = "retrieve the list of project of instance", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<Project>>> retrieveProjectList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<Project> assembler) {
        Page<Project> projects = projectService.retrieveProjectList(pageable);
        return ResponseEntity.ok(toPagedResources(projects, assembler));
    }

    /**
     * Retrieve projects list
     *
     * @return List of projects
     */
    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    @ResourceAccess(description = "retrieve the list of project of instance", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<Project>>> retrievePublicProjectList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<Project> assembler) {
        Page<Project> projects = projectService.retrievePublicProjectList(pageable);
        return ResponseEntity.ok(toPagedResources(projects, assembler));
    }

    /**
     * Create a new project
     *
     * @param newProject new Project to create
     * @return Created project
     * @throws ModuleException If Project already exists for the given name
     */
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResourceAccess(description = "create a new project", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Project>> createProject(@Valid @RequestBody Project newProject)
        throws ModuleException {
        Project project = projectService.createProject(newProject);
        return new ResponseEntity<>(toResource(project), HttpStatus.CREATED);
    }

    /**
     * Retrieve a project by name
     *
     * @param projectName Project name
     * @return Project
     * @throws ModuleException {@link EntityNotFoundException} project does not exists
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_name}", produces = "application/json")
    @ResourceAccess(description = "retrieve the project project_name", role = DefaultRole.PUBLIC)
    public ResponseEntity<EntityModel<Project>> retrieveProject(@PathVariable("project_name") String projectName)
        throws ModuleException {
        return ResponseEntity.ok(toResource(projectService.retrieveProject(projectName)));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_name}/license/reset")
    @ResourceAccess(description = "Allow instance admin to invalidate the license of a project for all the users of the project",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> resetLicense(@PathVariable("project_name") String projectName) {
        try {
            runtimeTenantResolver.forceTenant(projectName);
            FeignSecurityManager.asSystem();
            licenseClient.resetLicense();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } finally {
            runtimeTenantResolver.clearTenant();
            FeignSecurityManager.reset();
        }
    }

    /**
     * Update given project.
     *
     * @param projectName     project name
     * @param projectToUpdate project to update
     * @return Updated Project
     * @throws ModuleException {@link EntityNotFoundException} project does not exists
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{project_name}")
    @ResourceAccess(description = "update the project project_name", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Project>> updateProject(@PathVariable("project_name") String projectName,
                                                              @Valid @RequestBody Project projectToUpdate)
        throws ModuleException {
        Project project = projectService.updateProject(projectName, projectToUpdate);
        return ResponseEntity.ok(toResource(project));
    }

    /**
     * Delete given project
     *
     * @param projectName Project name to delete
     * @return Void
     * @throws ModuleException {@link EntityNotFoundException} project does not exists
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_name}")
    @ResourceAccess(description = "remove the project project_name", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> deleteProject(@PathVariable("project_name") String projectName) throws ModuleException {
        projectService.deleteProject(projectName);
        return ResponseEntity.noContent().build();
    }

    @Override
    public EntityModel<Project> toResource(Project project, Object... extras) {
        EntityModel<Project> resource = null;
        if (project != null && project.getName() != null) {
            resource = resourceService.toResource(project);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "retrieveProject",
                                    LinkRels.SELF,
                                    MethodParamFactory.build(String.class, project.getName()));
            if (!project.isDeleted()) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "deleteProject",
                                        LinkRels.DELETE,
                                        MethodParamFactory.build(String.class, project.getName()));
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "updateProject",
                                        LinkRels.UPDATE,
                                        MethodParamFactory.build(String.class, project.getName()),
                                        MethodParamFactory.build(Project.class, project));
            }
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "createProject",
                                    LinkRels.CREATE,
                                    MethodParamFactory.build(Project.class, project));
        } else {
            LOG.warn(String.format("Invalid %s entity. Cannot create hateoas resources", this.getClass().getName()));
        }
        return resource;
    }
}
