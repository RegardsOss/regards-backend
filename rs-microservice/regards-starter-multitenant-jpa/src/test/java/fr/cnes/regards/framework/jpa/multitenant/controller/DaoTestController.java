/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.controller;

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

import fr.cnes.regards.framework.jpa.multitenant.pojo.User;
import fr.cnes.regards.framework.jpa.multitenant.repository.IUserRepository;

/**
 *
 * Class DaoTestController
 *
 * Test Rest controller to simulate access to DAO using scope (project) in authentication token. Used in Integraion
 * Tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping("/test/dao")
public class DaoTestController {

    /**
     * JPA User Repository. Access to Users in database
     */
    @Autowired
    private IUserRepository userRepo;

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
     * Retrieve all users from the project of the authenticated user.
     *
     * @return List<Users>
     * @throws CannotCreateTransactionException
     *             Error accessing project database
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public HttpEntity<List<User>> getUsers() throws CannotCreateTransactionException {
        final List<User> users = new ArrayList<>();
        userRepo.findAll().forEach(user -> users.add(user));
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

}
