/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.instance;

/**
 * Test controller for JWT and DAO Integration tests
 */
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/dao")
public class ProjectController {

    @Autowired
    @Qualifier("instanceEntityManagerFactory")
    EntityManager em;

    @Autowired
    private ProjectRepository projectRepo_;

    @ExceptionHandler(CannotCreateTransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void exception() {
    }

    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public HttpEntity<List<Project>> getUsers() throws CannotCreateTransactionException {
        List<Project> projects = new ArrayList<>();
        projectRepo_.findAll().forEach(project -> projects.add(project));
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

}
