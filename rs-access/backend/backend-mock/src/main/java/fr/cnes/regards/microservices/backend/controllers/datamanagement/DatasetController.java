package fr.cnes.regards.microservices.backend.controllers.datamanagement;

import fr.cnes.regards.microservices.backend.pojo.Account;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.websocket.server.PathParam;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class DatasetController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/users@GET", new RoleAuthority("ADMIN"));
    }

    @RequestMapping(value = "/azertyuiop", method = RequestMethod.GET)
    public HttpEntity<List<Account>> getProjectUsers() {
        List<Account> accounts = new ArrayList<>();
        AtomicLong counter = new AtomicLong();


        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @RequestMapping(value = "/azertyuiop/{{user_id}}", method = RequestMethod.GET)
    public @ResponseBody HttpEntity<Account> getProjectUser(@PathParam("user_id") Integer userId) {
        AtomicLong counter = new AtomicLong();
        Account user = new Account(counter.incrementAndGet(), "John", "Constantine", "john.constantine@...", "jconstantine", "passw0rd");

        return new ResponseEntity<>(user, HttpStatus.OK);
    }


}
