/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.hateoas.HateoasKeyWords;
import fr.cnes.regards.modules.core.rest.Controller;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.project.signature.IProjectsSignature;

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
@ModuleInfo(name = "project", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class ProjectsController extends Controller implements IProjectsSignature {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectsController.class);

    /**
     * Business service for Project entities. Autowired.
     */
    private final IProjectService projectService;

    public ProjectsController(final IProjectService pProjectService) {
        super();
        projectService = pProjectService;
    }

    // @Override
    // @ExceptionHandler({ EntityNotFoundException.class, NoSuchElementException.class })
    // @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    // public void dataNotFound() {
    // }
    //
    // @Override
    // @ExceptionHandler(AlreadyExistingException.class)
    // @ResponseStatus(value = HttpStatus.CONFLICT)
    // public void dataAlreadyExisting() {
    // }
    //
    // @ExceptionHandler(InvalidEntityException.class)
    // @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    // public void operationNotSupported() {
    // }

    @Override
    @ResourceAccess(description = "retrieve the list of project of instance")
    public HttpEntity<List<Resource<Project>>> retrieveProjectList() {

        final List<Project> projects = projectService.retrieveProjectList();
        final List<Resource<Project>> resources = projects.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        addLinksToProjects(resources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "create a new project")
    public HttpEntity<Resource<Project>> createProject(@Valid @RequestBody final Project pNewProject)
            throws EntityException {

        final Project project = projectService.createProject(pNewProject);
        final Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "retrieve the project project_name")
    public HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_name") final String pProjectName)
            throws EntityException {

        final Project project = projectService.retrieveProject(pProjectName);
        final Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the project project_name")
    public HttpEntity<Resource<Project>> updateProject(@PathVariable("project_name") final String pProjectName,
            @RequestBody final Project pProjectToUpdate) throws EntityException {

        final Project project = projectService.updateProject(pProjectName, pProjectToUpdate);
        final Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove the project project_name")
    public HttpEntity<Void> deleteProject(@PathVariable("project_name") final String pProjectName)
            throws EntityException {

        projectService.deleteProject(pProjectName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> retrieveProjectConnection(
            @PathVariable("project_name") final String pProjectName,
            @PathVariable("microservice") final String pMicroService) throws EntityNotFoundException {

        final ResponseEntity<Resource<ProjectConnection>> response;
        final ProjectConnection pConn = projectService.retreiveProjectConnection(pProjectName, pMicroService);

        if (pConn != null) {
            final Resource<ProjectConnection> resource = new Resource<ProjectConnection>(pConn);
            addLinksToProjectConnection(resource);
            response = new ResponseEntity<>(resource, HttpStatus.OK);
        } else {
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> createProjectConnection(
            @Valid @RequestBody final ProjectConnection pProjectConnection) throws EntityException {
        final ProjectConnection pConn = projectService.createProjectConnection(pProjectConnection);
        final Resource<ProjectConnection> resource = new Resource<ProjectConnection>(pConn);
        addLinksToProjectConnection(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> updateProjectConnection(
            @Valid @RequestBody final ProjectConnection pProjectConnection) throws EntityException {
        final ProjectConnection pConn = projectService.updateProjectConnection(pProjectConnection);
        final Resource<ProjectConnection> resource = new Resource<ProjectConnection>(pConn);
        addLinksToProjectConnection(resource);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> deleteProjectConnection(@PathVariable("project_name") final String pProjectName,
            @PathVariable("microservice") final String pMicroservice) throws EntityException {

        final ResponseEntity<Void> response;
        final ProjectConnection pConn = projectService.retreiveProjectConnection(pProjectName, pMicroservice);
        if (pConn != null) {
            projectService.deleteProjectConnection(pConn.getId());
            response = new ResponseEntity<>(HttpStatus.OK);
        } else {
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;
    }

    /**
     *
     * Add Hateoas links to a ProjectConnection resource.
     *
     * @param pProjectConnection
     *            Resource<ProjectConnection> resource
     * @since 1.0-SNAPSHOT
     */
    private static void addLinksToProjectConnection(final Resource<ProjectConnection> pProjectConnection) {
        if (pProjectConnection.getLinks().isEmpty()) {
            try {
                // Add self link
                pProjectConnection
                        .add(ControllerLinkBuilder
                                .linkTo(ControllerLinkBuilder.methodOn(ProjectsController.class)
                                        .retrieveProjectConnection(pProjectConnection.getContent().getProject()
                                                .getName(), pProjectConnection.getContent().getMicroservice()))
                                .withSelfRel());
                // Add delete link
                pProjectConnection.add(ControllerLinkBuilder
                        .linkTo(ControllerLinkBuilder.methodOn(ProjectsController.class)
                                .deleteProjectConnection(pProjectConnection.getContent().getProject().getName(),
                                                         pProjectConnection.getContent().getMicroservice()))
                        .withRel(HateoasKeyWords.DELETE.getValue()));

                // Add update link
                pProjectConnection.add(ControllerLinkBuilder
                        .linkTo(ControllerLinkBuilder.methodOn(ProjectsController.class)
                                .updateProjectConnection(pProjectConnection.getContent()))
                        .withRel(HateoasKeyWords.UPDATE.getValue()));
                // Add create link

                pProjectConnection.add(ControllerLinkBuilder
                        .linkTo(ControllerLinkBuilder.methodOn(ProjectsController.class)
                                .createProjectConnection(pProjectConnection.getContent()))
                        .withRel(HateoasKeyWords.CREATE.getValue()));
            } catch (final EntityException e) {
                // Nothing to do. Method is not called
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    /**
     *
     * Add Hateoas links to a Project resource.
     *
     * @param pProject
     *            Resource<Project> resource
     * @since 1.0-SNAPSHOT
     */
    private static void addLinksToProject(final Resource<Project> pProject) {
        if (pProject.getLinks().isEmpty()) {
            try {
                pProject.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ProjectsController.class)
                        .retrieveProject(pProject.getContent().getName())).withSelfRel());
                // project.add(linkTo(methodOn(ProjectsController.class).modifyProject(project.getName(), project))
                // .withRel("update"));
                pProject.add(ControllerLinkBuilder
                        .linkTo(ControllerLinkBuilder.methodOn(ProjectsController.class)
                                .deleteProject(pProject.getContent().getName()))
                        .withRel(HateoasKeyWords.DELETE.getValue()));
            } catch (final EntityException e) {
                // Nothing to do. Method is not called
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    /**
     *
     * Add Hateoas links to a list of Project resources.
     *
     * @param pProjects
     *            List<Resource<Project>> resources
     * @since 1.0-SNAPSHOT
     */
    private static void addLinksToProjects(final List<Resource<Project>> pProjects) {
        for (final Resource<Project> project : pProjects) {
            addLinksToProject(project);
        }
    }
}
