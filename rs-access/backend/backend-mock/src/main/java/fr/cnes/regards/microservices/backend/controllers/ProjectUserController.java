package fr.cnes.regards.microservices.backend.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.websocket.server.PathParam;

import fr.cnes.regards.microservices.backend.pojo.ProjectUser;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.Account;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectUserController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/project/users@GET", new RoleAuthority("ADMIN"));
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public HttpEntity<List<Account>> getProjectUsers() {
        List<Account> accounts = new ArrayList<>();

        List<ProjectUser> pUsers = new ArrayList();
        pUsers.add(new ProjectUser());
        Account account = new Account("John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");
        account.setProjectUsers(pUsers);
        account.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("John")).withSelfRel());
        account.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("John")).withRel("role"));
        account.add(linkTo(methodOn(ProjectController.class).getProject("John")).withRel("projet"));
        account.add(linkTo(methodOn(ProjectController.class).getProject("John")).withRel("projetUser"));
        accounts.add(account);

        Account account2 = new Account("Foo", "Bar", "fbar@...", "fbar", "passw0rd");
        account2.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("Foo")).withSelfRel());
        account2.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("Foo")).withRel("role"));
        account2.add(linkTo(methodOn(ProjectController.class).getProject("Foo")).withRel("projet"));
        account2.add(linkTo(methodOn(ProjectController.class).getProject("Foo")).withRel("projetUser"));
        accounts.add(account2);

        return new ResponseEntity<List<Account>>(accounts, HttpStatus.OK);
    }

    @RequestMapping(value = "/users/{{user_id}}", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<Account> getProjectUser(@PathParam("user_id") Integer userId) {

        Account user = new Account("John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");

        return new ResponseEntity<Account>(user, HttpStatus.OK);
    }
    /*
    @RequestMapping(value = "/users/{{user_id}}/permissions", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<Account> getUserPermissions(@PathParam("user_id") Integer userId) {

        Account user = new Account("John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");

        return new ResponseEntity<Account>(user, HttpStatus.OK);
    }*/


}
