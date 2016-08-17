package fr.cnes.regards.microservices.backend.controllers.administration;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.Account;
import fr.cnes.regards.microservices.backend.pojo.ProjectAccount;
import fr.cnes.regards.microservices.backend.pojo.Role;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

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
    public void init() {
        authService_.setAutorities("/api/users@GET", new RoleAuthority("ADMIN"));

    }

    @RequestMapping(value = "/projectAccounts", method = RequestMethod.GET)
    public HttpEntity<List<ProjectAccount>> getProjectAccounts() {
        return new ResponseEntity<>(getInMemotyProjectAccounts(), HttpStatus.OK);
    }

    @RequestMapping(value = "/projectAccounts/{account_id}", method = RequestMethod.GET)
    public HttpEntity<ProjectAccount> getProjectAccount(@PathVariable("account_id") Long accountId) {
        ProjectAccount pa = null;
        for (ProjectAccount projectAccount : getInMemotyProjectAccounts()) {
            if (projectAccount.getProjectAccountId() == accountId.longValue()) {
                pa = projectAccount;
                break;
            }
        }
        return new ResponseEntity<>(pa, HttpStatus.OK);
    }

    @RequestMapping(value = "/projectAccounts/{account_id}", method = RequestMethod.DELETE)
    public HttpEntity<String> deleteProjectAccount(@PathVariable("account_id") Long accountId) {
        return new ResponseEntity<>("ProjectAccount deleted", HttpStatus.OK);
    }

    private List<ProjectAccount> getInMemotyProjectAccounts() {

        List<ProjectAccount> projectAccounts = new ArrayList<>();
        AtomicLong counter = new AtomicLong();

        ProjectAccount projectAccount = new ProjectAccount();
        projectAccount.setProjectAccountId(0L);

        Role role = projectAccount.getRole();
        role.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("john.constantine.role")).withSelfRel());

        Account account = new Account(counter.incrementAndGet(), "John", "Constantine", "john.constantine@...",
                "jconstantine", "passw0rd");
        projectAccount.add(linkTo(methodOn(ProjectAccountController.class)
                .deleteProjectAccount(projectAccount.getProjectAccountId())).withRel("delete"));
        projectAccount.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount.getProjectAccountId())).withSelfRel());
        projectAccount.setAccount(account);

        projectAccounts.add(projectAccount);

        ProjectAccount projectAccount2 = new ProjectAccount();
        projectAccount2.setProjectAccountId(1L);
        Account account2 = new Account(counter.incrementAndGet(), "Foo", "Bar", "fbar@...", "fbar", "passw0rd");
        projectAccount2.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount2.getProjectAccountId())).withSelfRel());
        projectAccount2.add(linkTo(methodOn(ProjectAccountController.class)
                .deleteProjectAccount(projectAccount2.getProjectAccountId())).withRel("delete"));
        projectAccount2.setAccount(account2);
        projectAccounts.add(projectAccount2);

        ProjectAccount projectAccount3 = new ProjectAccount();
        projectAccount3.setProjectAccountId(2L);
        Account account3 = new Account(counter.incrementAndGet(), "Instance", "admin", "admin@...", "admin",
                "passw0rd");
        projectAccount3.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount3.getProjectAccountId())).withSelfRel());
        projectAccount3.setAccount(account3);
        projectAccounts.add(projectAccount3);

        return projectAccounts;
    }

}
