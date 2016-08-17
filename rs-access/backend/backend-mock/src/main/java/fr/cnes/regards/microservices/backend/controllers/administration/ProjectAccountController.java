package fr.cnes.regards.microservices.backend.controllers.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.websocket.server.PathParam;

import fr.cnes.regards.microservices.backend.pojo.ProjectAccount;
import fr.cnes.regards.microservices.backend.pojo.Role;
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
public class ProjectAccountController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/users@GET", new RoleAuthority("ADMIN"));
    }

    @RequestMapping(value = "/projectAccounts", method = RequestMethod.GET)
    public HttpEntity<List<Account>> getProjectUsers() {
        List<Account> accounts = new ArrayList<>();
        AtomicLong counter = new AtomicLong();
        List<ProjectAccount> pAccounts = new ArrayList();
        ProjectAccount projectAccount = new ProjectAccount();
        projectAccount.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("john.constantine.pu")).withSelfRel());
        Role role = projectAccount.getRole();
        role.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("john.constantine.role")).withSelfRel());
        pAccounts.add(projectAccount);
        Account account = new Account(counter.incrementAndGet(),"John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");
        account.setProjectAccounts(pAccounts);
        account.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("John")).withSelfRel());
        account.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("John")).withRel("role"));
        account.add(linkTo(methodOn(ProjectController.class).getProject("John")).withRel("projet"));
        account.add(linkTo(methodOn(ProjectController.class).getProject("John")).withRel("projetUser"));
        accounts.add(account);

        Account account2 = new Account(counter.incrementAndGet(), "Foo", "Bar", "fbar@...", "fbar", "passw0rd");
        account2.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("Foo")).withSelfRel());
        account2.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("Foo")).withRel("role"));
        account2.add(linkTo(methodOn(ProjectController.class).getProject("Foo")).withRel("projet"));
        account2.add(linkTo(methodOn(ProjectController.class).getProject("Foo")).withRel("projetUser"));
        accounts.add(account2);

        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @RequestMapping(value = "/projectAccounts/{{account_id}}", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<Account> getProjectUser(@PathParam("account_id") Integer accountId) {
        AtomicLong counter = new AtomicLong();
        Account user = new Account(counter.incrementAndGet(), "John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
    /*
    @RequestMapping(value = "/users/{{user_id}}/permissions", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<Account> getUserPermissions(@PathParam("user_id") Integer userId) {

        Account user = new Account("John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");

        return new ResponseEntity<Account>(user, HttpStatus.OK);
    }*/


}
