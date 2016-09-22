package fr.cnes.regards.microservices.backend.controllers.administration;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.sandbox.ProjectAdmin;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "project admin controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/api")
public class ProjectAdminController {

    @Autowired
    MethodAutorizationService authService_;

//    /**
//     * Method to initiate REST resources authorizations.
//     */
//    @PostConstruct
//    public void initAuthorisations() {
//        authService_.setAutorities("/api/project-admin@GET", new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/project-admins@GET", new RoleAuthority("ADMIN"));
//    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/project-admin", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<ProjectAdmin> getProjectAdmin(
            @RequestParam(value = "name", required = true) String name) {
        ProjectAdmin projectAdmin = new ProjectAdmin(name);
        projectAdmin.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin(name)).withSelfRel());
        return new ResponseEntity<>(projectAdmin, HttpStatus.OK);
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/project-admins", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<List<ProjectAdmin>> getProjectAdminsByNames(
            @RequestParam(value = "names", required = true) String[] names) {
        List<ProjectAdmin> projectAdmins = new ArrayList<>(names.length);

        for (String name : names) {
            ProjectAdmin projectAdmin = new ProjectAdmin(name);
            projectAdmin.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin(name)).withSelfRel());
            projectAdmins.add(projectAdmin);
        }

        return new ResponseEntity<>(projectAdmins, HttpStatus.OK);
    }

}
