/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microserices.administration.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Class ProjectClientStub
 *
 * Stub to get a test implementation for IProjectsClient
 * @author Sébastien Binda
 */
public class ProjectClientStub implements IProjectsClient {

    /**
     * Project name return by this stub
     */
    public static final String PROJECT_NAME = "stub-project";

    /**
     * Project managed by this stub
     */
    public static final Project PROJECT = new Project(0L, "", "", true, PROJECT_NAME);

    @Override
    public ResponseEntity<PagedModel<EntityModel<Project>>> retrieveProjectList(final int pPage, final int pSize) {
        List<EntityModel<Project>> resources = new ArrayList<>();
        resources.add(new EntityModel<>(PROJECT));
        PagedModel<EntityModel<Project>> page = new PagedModel<>(resources, new PageMetadata(pSize, pPage, 1),
                new ArrayList<>());
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<Project>>> retrievePublicProjectList(int page, int size) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<EntityModel<Project>> createProject(Project pNewProject) {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<EntityModel<Project>> retrieveProject(String pProjectName) {
        return new ResponseEntity<>(new EntityModel<>(PROJECT), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<Project>> updateProject(String pProjectName, Project pProjectToUpdate) {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Void> deleteProject(String pProjectName) {
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
