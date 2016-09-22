package fr.cnes.regards.microservices.backend.controllers.administration;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.administration.Account;
import fr.cnes.regards.microservices.backend.pojo.administration.ProjectAccount;
import fr.cnes.regards.microservices.backend.pojo.administration.Role;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;

@RestController
@ModuleInfo(name = "project account controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/api")
public class ProjectAccountController {

    // Mock db
    static List<ProjectAccount> inMemoryPAList = null;
    @Autowired
    MethodAutorizationService authService_;

//    /**
//     * Method to initiate REST resources authorizations.
//     */
//    @PostConstruct
//    public void init() {
//        authService_.setAutorities("/api/users@GET", new RoleAuthority("ADMIN"));
//
//    }

    @RequestMapping(value = "/projectAccounts", method = RequestMethod.GET)
    public HttpEntity<List<ProjectAccount>> getProjectAccounts() {
        return new ResponseEntity<>(getInMemoryProjectAccounts(), HttpStatus.OK);
    }

    @RequestMapping(value = "/projectAccounts/{account_id}", method = RequestMethod.GET)
    public HttpEntity<ProjectAccount> getProjectAccount(@PathVariable("account_id") Long accountId) {
        ProjectAccount pa = null;
        for (ProjectAccount projectAccount : getInMemoryProjectAccounts()) {
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

    private List<ProjectAccount> getInMemoryProjectAccounts() {
        if (inMemoryPAList != null) {
            return inMemoryPAList;
        }
        inMemoryPAList = new ArrayList<>();
        AtomicLong counter = new AtomicLong();


    	
        

        ProjectAccount projectAccount = new ProjectAccount();
        projectAccount.setProjectAccountId(0L);
        Account account = new Account(counter.incrementAndGet(), "Nicolas", "Dufourg", "Nicolas.Dufourg@cnes.fr", "ndufourg", "passw0rd");
        Role role = projectAccount.getRole();
        role.add(linkTo(methodOn(ProjectAdminController.class).getProjectAdmin("dominique.heulet.role")).withSelfRel());
        projectAccount.add(linkTo(methodOn(ProjectAccountController.class)
                .deleteProjectAccount(projectAccount.getProjectAccountId())).withRel("delete"));
        projectAccount.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount.getProjectAccountId())).withSelfRel());
        projectAccount.setAccount(account);
        inMemoryPAList.add(projectAccount);

        ProjectAccount projectAccount2 = new ProjectAccount();
        projectAccount2.setProjectAccountId(1L);
        Account account2 = new Account(counter.incrementAndGet(), "Benoit", "Lavraud", "Benoit.Lavraud@irap.omp.eu", "blavraud", "passw0rd");
        projectAccount2.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount2.getProjectAccountId())).withSelfRel());
        projectAccount2.add(linkTo(methodOn(ProjectAccountController.class)
                .deleteProjectAccount(projectAccount2.getProjectAccountId())).withRel("delete"));
        projectAccount2.setAccount(account2);
        inMemoryPAList.add(projectAccount2);

        ProjectAccount projectAccount3 = new ProjectAccount();
        projectAccount3.setProjectAccountId(2L);
        Account account3 = new Account(counter.incrementAndGet(), "Vincent", "Génot", "vincent.genot@cesr.fr", "vgenot", "passw0rd");
        projectAccount3.add(linkTo(methodOn(ProjectAccountController.class)
        		.getProjectAccount(projectAccount3.getProjectAccountId())).withSelfRel());
        projectAccount3.setAccount(account3);
        inMemoryPAList.add(projectAccount3);
        
        ProjectAccount projectAccount4 = new ProjectAccount();
        projectAccount4.setProjectAccountId(3L);
        Account account4 = new Account(counter.incrementAndGet(), "Ying", "Liu", "liu@phys.psu.edu", "ylui", "passw0rd");
        projectAccount4.add(linkTo(methodOn(ProjectAccountController.class)
        		.getProjectAccount(projectAccount4.getProjectAccountId())).withSelfRel());
        projectAccount4.setAccount(account4);
        inMemoryPAList.add(projectAccount4);
        
        ProjectAccount projectAccount5 = new ProjectAccount();
        projectAccount5.setProjectAccountId(3L);
        Account account5 = new Account(counter.incrementAndGet(), " Dominique", "Heulet", "dominique.heulet@cnes.fr", "admin", "passw0rd");
        projectAccount5.add(linkTo(methodOn(ProjectAccountController.class)
                .getProjectAccount(projectAccount5.getProjectAccountId())).withSelfRel());
        projectAccount5.setAccount(account5);
        inMemoryPAList.add(projectAccount5);

        return inMemoryPAList;
    }

}
