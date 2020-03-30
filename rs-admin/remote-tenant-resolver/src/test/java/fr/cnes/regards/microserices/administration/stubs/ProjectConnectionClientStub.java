/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 * Class ProjectClientStub
 *
 * Stub class for administration service client.
 * @author Sébastien Binda
 */
public class ProjectConnectionClientStub implements IProjectConnectionClient {

    @Override
    public ResponseEntity<PagedModel<EntityModel<ProjectConnection>>> getAllProjectConnections(String pProjectName) {

        List<EntityModel<ProjectConnection>> resources = new ArrayList<>();
        ProjectConnection connection = new ProjectConnection(0L, ProjectClientStub.PROJECT, "MICROSERVICE", "", "", "",
                "");
        resources.add(new EntityModel<>(connection));

        PagedModel<EntityModel<ProjectConnection>> page = new PagedModel<>(resources, new PageMetadata(1, 1, 1),
                new ArrayList<>());
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<ProjectConnection>> getProjectConnection(String pProjectName,
            Long pConnectionId) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectConnection>> createProjectConnection(String pProjectName,
            ProjectConnection pProjectConnection) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<ProjectConnection>> updateProjectConnection(String pProjectName,
            Long pConnectionId, ProjectConnection pProjectConnection) {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteProjectConnection(String pProjectName, Long pConnectionId) {
        return null;
    }

}
