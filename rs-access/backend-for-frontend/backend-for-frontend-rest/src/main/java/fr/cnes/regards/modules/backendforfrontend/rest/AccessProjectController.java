package fr.cnes.regards.modules.backendforfrontend.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Controller proxying ProjectController from rs-admin-instance by allowing resource access rights
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(path = AccessProjectController.ROOT_MAPPING)
public class AccessProjectController {

    public static final String ROOT_MAPPING = "/projects";

    @Autowired
    private IProjectsClient projectClient;

    @RequestMapping(method = RequestMethod.GET, value = "/{project_name}", produces = "application/json")
    @ResponseBody
    @ResourceAccess(description = "retrieve the project project_name", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<Project>> retrieveProject(@PathVariable("project_name") final String projectName)
            throws ModuleException {
        FeignSecurityManager.asSystem();
        ResponseEntity<Resource<Project>> project = projectClient.retrieveProject(projectName);
        FeignSecurityManager.reset();
        return project;

    }

    @RequestMapping(value = "/public", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResourceAccess(description = "retrieve the list of project of instance", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<Project>>> retrievePublicProjectList(final Pageable pageable) {
        FeignSecurityManager.asSystem();
        ResponseEntity<PagedResources<Resource<Project>>> publicProjects = projectClient.retrievePublicProjectList(pageable.getPageNumber(), pageable.getPageSize());
        FeignSecurityManager.reset();
        return publicProjects;
    }
}
