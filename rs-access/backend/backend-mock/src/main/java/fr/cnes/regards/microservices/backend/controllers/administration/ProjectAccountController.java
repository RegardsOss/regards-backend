package fr.cnes.regards.microservices.backend.controllers.administration;

import fr.cnes.regards.microservices.backend.pojo.administration.Account;
import fr.cnes.regards.microservices.backend.pojo.administration.ProjectUser;
import fr.cnes.regards.microservices.backend.pojo.administration.Role;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class ProjectAccountController {

    // Mock db
    static List<ProjectUser> inMemoryPAList = null;
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
    public HttpEntity<List<ProjectUser>> getProjectAccounts() {
        return new ResponseEntity<>(getInMemoryProjectAccounts(), HttpStatus.OK);
    }

    @RequestMapping(value = "/projectAccounts/{account_id}", method = RequestMethod.GET)
    public HttpEntity<ProjectUser> getProjectAccount(@PathVariable("account_id") Long accountId) {
        ProjectUser pa = null;
        for (ProjectUser projectAccount : getInMemoryProjectAccounts()) {
            if (projectAccount.getProjectAccountId() == accountId.longValue()) {
                pa = projectAccount;
                break;
            }
        }
        return new ResponseEntity<>(pa, HttpStatus.OK);
    }

    @RequestMapping(value = "/projectAccounts/{account_id}", method = RequestMethod.DELETE)
    public HttpEntity<String> deleteProjectAccount(@PathVariable("account_id") Long accountId) {

        return new ResponseEntity<>("ProjectUser deleted", HttpStatus.OK);
    }

    private List<ProjectUser> getInMemoryProjectAccounts() {
        if (inMemoryPAList != null) {
            return inMemoryPAList;
        }
        inMemoryPAList = new ArrayList<>();
        AtomicLong counter = new AtomicLong();

        ProjectUser projectAccount = new ProjectUser();
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

        inMemoryPAList.add(projectAccount);

        ProjectUser projectAccount2 = new ProjectUser();
        projectAccount2.setProjectAccountId(1L);
        Account account2 = new Account(counter.incrementAndGet(), "Foo", "Bar", "fbar@...", "fbar", "passw0rd");
        projectAccount2.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount2.getProjectAccountId())).withSelfRel());
        projectAccount2.add(linkTo(methodOn(ProjectAccountController.class)
                .deleteProjectAccount(projectAccount2.getProjectAccountId())).withRel("delete"));
        projectAccount2.setAccount(account2);
        inMemoryPAList.add(projectAccount2);

        ProjectUser projectAccount3 = new ProjectUser();
        projectAccount3.setProjectAccountId(2L);
        Account account3 = new Account(counter.incrementAndGet(), "Instance", "admin", "admin@...", "admin",
                "passw0rd");
        projectAccount3.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount3.getProjectAccountId())).withSelfRel());
        projectAccount3.setAccount(account3);
        inMemoryPAList.add(projectAccount3);

        return inMemoryPAList;
    }

}
