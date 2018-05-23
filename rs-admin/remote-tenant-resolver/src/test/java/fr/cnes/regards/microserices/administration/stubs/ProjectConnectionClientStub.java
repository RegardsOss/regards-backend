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
package fr.cnes.regards.microserices.administration.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectClientStub
 *
 * Stub class for administration service client.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ProjectConnectionClientStub implements IProjectConnectionClient {

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient#getAllProjectConnections(java.lang.String)
     */
    @Override
    public ResponseEntity<PagedResources<Resource<ProjectConnection>>> getAllProjectConnections(String pProjectName) {

        final List<Resource<ProjectConnection>> resources = new ArrayList<>();
        final ProjectConnection connection = new ProjectConnection(0L, ProjectClientStub.PROJECT, "MICROSERVICE", "",
                "", "", "");
        resources.add(new Resource<ProjectConnection>(connection));

        final PagedResources<Resource<ProjectConnection>> page = new PagedResources<>(resources,
                new PageMetadata(1, 1, 1), new ArrayList<>());
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient#getProjectConnection(java.lang.String,
     * java.lang.Long)
     */
    @Override
    public ResponseEntity<Resource<ProjectConnection>> getProjectConnection(String pProjectName, Long pConnectionId) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient#createProjectConnection(java.lang.String,
     * fr.cnes.regards.modules.project.domain.ProjectConnection)
     */
    @Override
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(String pProjectName,
            ProjectConnection pProjectConnection) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient#updateProjectConnection(java.lang.String,
     * java.lang.Long, fr.cnes.regards.modules.project.domain.ProjectConnection)
     */
    @Override
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(String pProjectName, Long pConnectionId,
            ProjectConnection pProjectConnection) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.project.client.rest.IProjectConnectionClient#deleteProjectConnection(java.lang.String,
     * java.lang.Long)
     */
    @Override
    public ResponseEntity<Void> deleteProjectConnection(String pProjectName, Long pConnectionId) {
        return null;
    }

}
