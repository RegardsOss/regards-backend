/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.hateoas.HateoasKeyWords;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.project.signature.ProjectsSignature;
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "project", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class ProjectsController implements ProjectsSignature {

    @Autowired
    private IProjectService projectService;

    @ExceptionHandler({ EntityNotFoundException.class, NoSuchElementException.class })
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void dataNotFound() {
    }

    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public void operationNotSupported() {
    }

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
    public HttpEntity<Resource<Project>> createProject(@Valid @RequestBody final Project newProject)
            throws AlreadyExistingException {

        final Project project = projectService.createProject(newProject);
        final Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "retrieve the project project_id")
    public HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_id") final String projectId) {

        final Project project = projectService.retrieveProject(projectId);
        final Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the project project_id")
    public HttpEntity<Void> modifyProject(@PathVariable("project_id") final String projectId,
            @RequestBody final Project projectUpdated) throws OperationNotSupportedException, EntityNotFoundException {

        projectService.modifyProject(projectId, projectUpdated);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove the project project_id")
    public HttpEntity<Void> deleteProject(@PathVariable("project_id") final String projectId) {

        projectService.deleteProject(projectId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> retrieveProjectConnection(
            @PathVariable("project_name") final String pProjectName,
            @PathVariable("microservice") final String pMicroService) {

        ResponseEntity<Resource<ProjectConnection>> response = null;
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
            @Valid @RequestBody final ProjectConnection pProjectConnection)
            throws AlreadyExistingException, EntityNotFoundException {
        final ProjectConnection pConn = projectService.createProjectConnection(pProjectConnection);
        final Resource<ProjectConnection> resource = new Resource<ProjectConnection>(pConn);
        addLinksToProjectConnection(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    public HttpEntity<Resource<ProjectConnection>> updateProjectConnection(
            @Valid @RequestBody final ProjectConnection pProjectConnection) throws EntityNotFoundException {
        final ProjectConnection pConn = projectService.updateProjectConnection(pProjectConnection);
        final Resource<ProjectConnection> resource = new Resource<ProjectConnection>(pConn);
        addLinksToProjectConnection(resource);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> deleteProjectConnection(@PathVariable("project_name") final String pProjectName,
            @PathVariable("microservice") final String pMicroservice) throws EntityNotFoundException {

        ResponseEntity<Void> response;
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
                pProjectConnection.add(linkTo(methodOn(ProjectsController.class)
                        .retrieveProjectConnection(pProjectConnection.getContent().getProject().getName(),
                                                   pProjectConnection.getContent().getMicroservice())).withSelfRel());
                // Add delete link
                pProjectConnection.add(linkTo(methodOn(ProjectsController.class)
                        .deleteProjectConnection(pProjectConnection.getContent().getProject().getName(),
                                                 pProjectConnection.getContent().getMicroservice()))
                                                         .withRel(HateoasKeyWords.DELETE.getValue()));

                // Add update link
                pProjectConnection.add(linkTo(methodOn(ProjectsController.class)
                        .updateProjectConnection(pProjectConnection.getContent()))
                                .withRel(HateoasKeyWords.UPDATE.getValue()));
                // Add create link

                pProjectConnection.add(linkTo(methodOn(ProjectsController.class)
                        .createProjectConnection(pProjectConnection.getContent()))
                                .withRel(HateoasKeyWords.CREATE.getValue()));
            } catch (final AlreadyExistingException | EntityNotFoundException e) {
                // Nothing to do. Method is not called
            }
        }
    }

    /**
     *
     * Add Hateoas links to a Project resource.
     *
     * @param project
     *            Resource<Project> resource
     * @since 1.0-SNAPSHOT
     */
    private static void addLinksToProject(final Resource<Project> project) {
        if (project.getLinks().isEmpty()) {
            project.add(linkTo(methodOn(ProjectsController.class).retrieveProject(project.getContent().getName()))
                    .withSelfRel());
            // project.add(linkTo(methodOn(ProjectsController.class).modifyProject(project.getName(), project))
            // .withRel("update"));
            project.add(linkTo(methodOn(ProjectsController.class).deleteProject(project.getContent().getName()))
                    .withRel(HateoasKeyWords.DELETE.getValue()));
        }
    }

    /**
     *
     * Add Hateoas links to a list of Project resources.
     *
     * @param projects
     *            List<Resource<Project>> resources
     * @since 1.0-SNAPSHOT
     */
    private static void addLinksToProjects(final List<Resource<Project>> projects) {
        for (final Resource<Project> project : projects) {
            addLinksToProject(project);
        }
    }
}
