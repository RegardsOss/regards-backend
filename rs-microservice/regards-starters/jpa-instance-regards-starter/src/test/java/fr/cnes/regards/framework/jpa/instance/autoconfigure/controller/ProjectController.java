/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure.controller;

/**
 * Test controller for JWT and DAO Integration tests
 */
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.jpa.instance.autoconfigure.pojo.TestProject;
import fr.cnes.regards.framework.jpa.instance.autoconfigure.repository.IProjectTestRepository;

/**
 *
 * Class ProjectController
 *
 * Test Rest controller to simulate access to DAO using scope (project) in authentication token. Used in Integraion
 * Tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping("/test/dao")
public class ProjectController {

    /**
     * JPA User Repository. Access to Projects in database
     */
    @Autowired
    private IProjectTestRepository projectRepo;

    /**
     *
     * Exception handler for this REST Controller
     *
     * @since 1.0-SNAPSHOT
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void exception() {
    }

    /**
     *
     * Retrieve all projects from the project of the authenticated user.
     *
     * @return List<Projects>
     * @throws CannotCreateTransactionException
     *             Error accessing project database
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public HttpEntity<List<TestProject>> getUsers() throws CannotCreateTransactionException {
        final List<TestProject> projects = new ArrayList<>();
        projectRepo.findAll().forEach(project -> projects.add(project));
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

}
