/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;

/**
 *
 * Class ProjectsController
 *
 * Endpoints to manage the projects databases name ProjectConnection
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "project", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping(ProjectConnectionController.TYPE_MAPPING)
public class ProjectConnectionController implements IResourceController<ProjectConnection> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConnectionController.class);
    
    /**
     * Type mapping
     */
    public static final String TYPE_MAPPING = "/project_connections";

    /**
     * Business service for Project entities. Autowired.
     */
    private final IProjectConnectionService projectConnectionService;

    /**
     * Resource service to manage visibles hateoas links
     */
    private final IResourceService resourceService;

    public ProjectConnectionController(final IProjectConnectionService pProjectConnectionService,
            final IResourceService pResourceService) {
        super();
        projectConnectionService = pProjectConnectionService;
        resourceService = pResourceService;
    }

    /**
     *
     * Retrieve all project connections in instance database.
     *
     * @param pProjectConnection
     *            ProjectConnection to create.
     * @return The list of project connections wrapped in a response entity of pages resources
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResourceAccess(description = "Retrieve all projects connections", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedResources<Resource<ProjectConnection>>> retrieveProjectsConnections(
            final Pageable pPageable, final PagedResourcesAssembler<ProjectConnection> pAssembler) {
        final Page<ProjectConnection> connections = projectConnectionService.retrieveProjectsConnections(pPageable);
        return ResponseEntity.ok(toPagedResources(connections, pAssembler));
    }

    /**
     *
     * Retrieve the project connection of passed id.
     *
     * @param pId
     *            The id
     * @return The project connection wrapped in a response entity of pages resources
     * @throws EntityNotFoundException
     *             Project connection doesn't exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{project_connection_id}", produces = "application/json")
    @ResponseBody
    @ResourceAccess(description = "Retreieve the project connection of passed id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnectionById(
            @PathVariable("project_connection_id") final Long pId) throws EntityNotFoundException {
        final ProjectConnection connection = projectConnectionService.retrieveProjectConnectionById(pId);
        return ResponseEntity.ok(toResource(connection));
    }

    /**
     * Retrieve all project connections from database for a given project/tenant.
     *
     * @param pProjectName
     *            The project name
     * @param pPageable
     *            Spring managed object containing pagination information
     * @param pAssembler
     *            Spring managed resource assembler to convert {@link Page} into {@link PagedResources}
     * @return The list of project connections wrapped in a response entity of pages resources
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "?project_name={project_name}", produces = "application/json")
    @ResponseBody
    @ResourceAccess(description = "Retrieve all projects connections for a given project/tenant", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedResources<Resource<ProjectConnection>>> retrieveProjectsConnectionsByProjectName(
            @RequestParam("project_name") final String pProjectName, final Pageable pPageable,
            final PagedResourcesAssembler<ProjectConnection> pAssembler) {
        final Page<ProjectConnection> connections = projectConnectionService
                .retrieveProjectsConnectionsByProject(pProjectName, pPageable);
        return ResponseEntity.ok(toPagedResources(connections, pAssembler));
    }

    /**
     *
     * Retrieve a project connection from instance database for a given project and a given microservice.
     *
     * @param pProjectName
     *            Project name
     * @param pMicroService
     *            Microservice name
     * @return HttpEntity<Resource<ProjectConnection>>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, value = "?project_name={project_name}&microservice={microservice}")
    @ResponseBody
    @ResourceAccess(description = "retrieve a project connection associated to a given project and a given microservice", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(
            @RequestParam("project_name") final String pProjectName,
            @RequestParam("microservice") final String pMicroService) {

        ResponseEntity<Resource<ProjectConnection>> response;
        try {
            final ProjectConnection pConn = projectConnectionService.retrieveProjectConnection(pProjectName,
                                                                                               pMicroService);
            if (pConn != null) {
                response = ResponseEntity.ok(toResource(pConn));
            } else {
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (final EntityNotFoundException e) {
            LOG.debug(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    /**
     *
     * Create a new project connection in instance database. The associated Project must exists and have a valid
     * identifier.
     *
     * @param pProjectConnection
     *            ProjectConnection to create.
     * @return ProjectConnection created
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "create a new project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            @Valid @RequestBody final ProjectConnection pProjectConnection) {
        ResponseEntity<Resource<ProjectConnection>> response;
        try {
            final ProjectConnection pConn = projectConnectionService.createProjectConnection(pProjectConnection, false);
            response = new ResponseEntity<>(toResource(pConn), HttpStatus.CREATED);
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    /**
     *
     * Update an existing Project connection
     *
     * @param pProjectConnection
     *            ProjectConnection to update
     * @return updated pProjectConnection
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{project_connection_id}")
    @ResponseBody
    @ResourceAccess(description = "update a project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(@PathVariable("project_connection_id") final Long pId,
            @Valid @RequestBody final ProjectConnection pProjectConnection) {
        ResponseEntity<Resource<ProjectConnection>> response;
        try {
            final ProjectConnection pConn = projectConnectionService.updateProjectConnection(pProjectConnection);
            response = ResponseEntity.ok(toResource(pConn));
        } catch (final EntityNotFoundException e) {
            LOG.debug(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;
    }

    /**
     *
     * Delete an existing Project connection
     *
     * @param pProjectName
     *            project name
     * @param pMicroservice
     *            microservice name
     * @return void
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseBody
    @ResourceAccess(description = "delete a project connection", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> deleteProjectConnection(@RequestParam("project_name") final String pProjectName,
            @RequestParam("microservice") final String pMicroservice) {

        ResponseEntity<Void> response;
        try {
            final ProjectConnection pConn = projectConnectionService.retrieveProjectConnection(pProjectName,
                                                                                               pMicroservice);
            if (pConn != null) {
                projectConnectionService.deleteProjectConnection(pConn.getId());
                response = new ResponseEntity<>(HttpStatus.OK);
            } else {
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (final EntityNotFoundException e) {
            LOG.debug(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    @Override
    public Resource<ProjectConnection> toResource(final ProjectConnection pElement, final Object... pExtras) {

        final Resource<ProjectConnection> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveProjectConnection", LinkRels.SELF,
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(String.class, pElement.getMicroservice()));
        resourceService.addLink(resource, this.getClass(), "updateProjectConnection", LinkRels.UPDATE,
                                MethodParamFactory.build(ProjectConnection.class, pElement));
        resourceService.addLink(resource, this.getClass(), "createProjectConnection", LinkRels.CREATE,
                                MethodParamFactory.build(ProjectConnection.class, pElement));
        resourceService.addLink(resource, this.getClass(), "deleteProjectConnection", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, pElement.getProject().getName()),
                                MethodParamFactory.build(String.class, pElement.getMicroservice()));

        return resource;
    }

}
