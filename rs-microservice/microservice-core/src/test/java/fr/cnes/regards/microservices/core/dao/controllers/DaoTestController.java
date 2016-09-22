package fr.cnes.regards.microservices.core.dao.controllers;

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

import fr.cnes.regards.microservices.core.dao.pojo.User;
import fr.cnes.regards.microservices.core.dao.repository.UserRepository;

@RestController
@RequestMapping("/test/dao")
public class DaoTestController {

    @Autowired
    private UserRepository userRepo_;

    @ExceptionHandler(CannotCreateTransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void exception() {
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public HttpEntity<List<User>> getUsers() throws CannotCreateTransactionException {
        List<User> users = new ArrayList<>();
        userRepo_.findAll().forEach(user -> users.add(user));
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

}
