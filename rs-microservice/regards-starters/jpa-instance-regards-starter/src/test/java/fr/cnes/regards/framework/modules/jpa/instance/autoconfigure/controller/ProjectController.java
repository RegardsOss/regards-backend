/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.controller;

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

import fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.pojo.TestProject;
import fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.repository.IProjectTestRepository;

/**
 * Class ProjectController
 *
 * Test Rest controller to simulate access to DAO using scope (project) in authentication token. Used in Integraion
 * Tests
 * @author CS
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
     * Exception handler for this REST Controller
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void exception() {
    }

    /**
     * Retrieve all projects from the project of the authenticated user.
     * @return List<Projects>
     * @throws CannotCreateTransactionException Error accessing project database
     */
    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public HttpEntity<List<TestProject>> getUsers() throws CannotCreateTransactionException {
        final List<TestProject> projects = new ArrayList<>();
        projectRepo.findAll().forEach(projects::add);
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

}
