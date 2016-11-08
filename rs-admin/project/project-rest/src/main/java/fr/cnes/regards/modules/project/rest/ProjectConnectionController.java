/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectConnectionService;
import fr.cnes.regards.modules.project.signature.IProjectConnectionSignature;

/**
 *
 * Class ProjectsController
 *
 * Controller for REST Access to Project entities
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "project", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
public class ProjectConnectionController
        implements IResourceController<ProjectConnection>, IProjectConnectionSignature {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectConnectionController.class);

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

    @Override
    @ResourceAccess(description = "retrieve a project connection associated to a given project and a given microservice")
    public ResponseEntity<Resource<ProjectConnection>> retrieveProjectConnection(
            @PathVariable("project_name") final String pProjectName,
            @PathVariable("microservice") final String pMicroService) throws EntityNotFoundException {

        final ResponseEntity<Resource<ProjectConnection>> response;
        final ProjectConnection pConn = projectConnectionService.retrieveProjectConnection(pProjectName, pMicroService);

        if (pConn != null) {
            response = ResponseEntity.ok(toResource(pConn));
        } else {
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;
    }

    @Override
    @ResourceAccess(description = "create a new project connection")
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            @Valid @RequestBody final ProjectConnection pProjectConnection) throws EntityException {
        final ProjectConnection pConn = projectConnectionService.createProjectConnection(pProjectConnection);
        return new ResponseEntity<>(toResource(pConn), HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "update a project connection")
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
            @Valid @RequestBody final ProjectConnection pProjectConnection) throws EntityException {
        final ProjectConnection pConn = projectConnectionService.updateProjectConnection(pProjectConnection);
        return ResponseEntity.ok(toResource(pConn));
    }

    @Override
    @ResourceAccess(description = "delete a project connection")
    public ResponseEntity<Void> deleteProjectConnection(@PathVariable("project_name") final String pProjectName,
            @PathVariable("microservice") final String pMicroservice) throws EntityException {

        final ResponseEntity<Void> response;
        final ProjectConnection pConn = projectConnectionService.retrieveProjectConnection(pProjectName, pMicroservice);
        if (pConn != null) {
            projectConnectionService.deleteProjectConnection(pConn.getId());
            response = new ResponseEntity<>(HttpStatus.OK);
        } else {
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;
    }

    @Override
    public Resource<ProjectConnection> toResource(final ProjectConnection pElement) {

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
