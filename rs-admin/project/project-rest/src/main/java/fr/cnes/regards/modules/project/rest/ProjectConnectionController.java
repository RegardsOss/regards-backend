/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;

/**
 *
 * Project connection API
 *
 * @author Marc Sordi
 *
 */
@RestController
@ModuleInfo(name = "project", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
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
     * @param projectName
     *            project name (i.e. tenant)
     * @param pPageable
     *            pageable
     * @param pAssembler
     *            assembler
     * @return all project connections
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all projects connections for a given project/tenant", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedResources<Resource<ProjectConnection>>> getAllProjectConnections(
            @PathVariable String projectName, Pageable pPageable,
            PagedResourcesAssembler<ProjectConnection> pAssembler) {
        Page<ProjectConnection> connections = projectConnectionService.retrieveProjectsConnectionsByProject(projectName,
                                                                                                            pPageable);
        return ResponseEntity.ok(toPagedResources(connections, pAssembler));
    }

    /**
     * Retrieve a single project connection by identifier
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            connection identifier
     * @return a project connection
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.GET, value = ProjectConnectionController.RESOURCE_ID_MAPPING)
    @ResourceAccess(description = "Retrieve a project connection of a given project/tenant", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<ProjectConnection>> getProjectConnection(@PathVariable String projectName,
            @PathVariable Long connectionId) throws ModuleException {
        ProjectConnection pConn = projectConnectionService.retrieveProjectConnectionById(connectionId);
        return ResponseEntity.ok(toResource(pConn));
    }

    /**
     * Test a single project connection by identifier
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            connection identifier
     * @return a project connection
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.GET, value = ProjectConnectionController.RESOURCE_ID_MAPPING + "/test")
    @ResourceAccess(description = "Test a project connection of a given project/tenant", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> testProjectConnection(@PathVariable String projectName, @PathVariable Long connectionId)
            throws ModuleException {
        projectConnectionService.testProjectConnection(connectionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Create a new project connection
     *
     * @param projectName
     *            project name
     * @param pProjectConnection
     *            connection to create
     * @return the create project connection
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a new project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(@PathVariable String projectName,
            @Valid @RequestBody final ProjectConnection pProjectConnection) throws ModuleException {
        ProjectConnection connection = projectConnectionService.createProjectConnection(pProjectConnection, false);
        return ResponseEntity.ok(toResource(connection));
    }

    /**
     * Update an existing project connection
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            project connection identifier
     * @param pProjectConnection
     *            project connection
     * @return updated connection
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.PUT, value = ProjectConnectionController.RESOURCE_ID_MAPPING)
    @ResourceAccess(description = "Update a project connection", role = DefaultRole.INSTANCE_ADMIN)

    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(@PathVariable String projectName,
            @PathVariable Long connectionId, @Valid @RequestBody final ProjectConnection pProjectConnection)
            throws ModuleException {
        ProjectConnection connection = projectConnectionService.updateProjectConnection(connectionId,
                                                                                        pProjectConnection);
        return ResponseEntity.ok(toResource(connection));
    }

    /**
     *
     * Delete an existing project connection
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            project connection identifier
     * @return {@link Void}
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.DELETE, value = ProjectConnectionController.RESOURCE_ID_MAPPING)
    @ResourceAccess(description = "delete a project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> deleteProjectConnection(@PathVariable String projectName,
            @PathVariable Long connectionId) throws ModuleException {
        projectConnectionService.deleteProjectConnection(connectionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public Resource<ProjectConnection> toResource(ProjectConnection pElement, Object... pExtras) {
        final Resource<ProjectConnection> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "getProjectConnection", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "testProjectConnection", "test",
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateProjectConnection", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(ProjectConnection.class));
        resourceService.addLink(resource, this.getClass(), "deleteProjectConnection", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "getAllProjectConnections", LinkRels.LIST,
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        return resource;
    }
}
