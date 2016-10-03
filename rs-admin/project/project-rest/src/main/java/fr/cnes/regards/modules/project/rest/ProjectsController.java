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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.project.signature.ProjectsSignature;

@RestController
@ModuleInfo(name = "project", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/projects")
public class ProjectsController implements ProjectsSignature {

    @Autowired
    private IProjectService projectService;

    @ExceptionHandler(NoSuchElementException.class)
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

        List<Project> projects = projectService.retrieveProjectList();
        List<Resource<Project>> resources = projects.stream().map(p -> new Resource<>(p)).collect(Collectors.toList());
        addLinksToProjects(resources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "create a new project")
    public HttpEntity<Resource<Project>> createProject(@Valid @RequestBody Project newProject)
            throws AlreadyExistingException {

        Project project = projectService.createProject(newProject);
        Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    @ResourceAccess(description = "retrieve the project project_id")
    public HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_id") String projectId) {

        Project project = projectService.retrieveProject(projectId);
        Resource<Project> resource = new Resource<Project>(project);
        addLinksToProject(resource);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "update the project project_id")
    public HttpEntity<Void> modifyProject(@PathVariable("project_id") String projectId,
            @RequestBody Project projectUpdated) throws OperationNotSupportedException {

        projectService.modifyProject(projectId, projectUpdated);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @ResourceAccess(description = "remove the project project_id")
    public HttpEntity<Void> deleteProject(@PathVariable("project_id") String projectId) {

        projectService.deleteProject(projectId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public static void addLinksToProject(Resource<Project> project) {
        if (project.getLinks().isEmpty()) {
            project.add(linkTo(methodOn(ProjectsController.class).retrieveProject(project.getContent().getName()))
                    .withSelfRel());
            // project.add(linkTo(methodOn(ProjectsController.class).modifyProject(project.getName(), project))
            // .withRel("update"));
            project.add(linkTo(methodOn(ProjectsController.class).deleteProject(project.getContent().getName()))
                    .withRel("delete"));
        }
    }

    public static void addLinksToProjects(List<Resource<Project>> projects) {
        for (Resource<Project> project : projects) {
            addLinksToProject(project);
        }
    }
}
