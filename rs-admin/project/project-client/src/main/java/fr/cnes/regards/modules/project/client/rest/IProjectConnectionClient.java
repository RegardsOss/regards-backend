/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.project.client.rest;

import javax.validation.Valid;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectsClient
 *
 * Feign client allowing access to the module with REST requests.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin-instance")
@RequestMapping(value = "/projects/{projectName}/connections", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IProjectConnectionClient {

    /**
     * Retrieve all project connections
     *
     * @param projectName
     *            project name (i.e. tenant)
     * @param pPageable
     *            pageable
     * @param pAssembler
     *            assembler
     * @return all project connections
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<ProjectConnection>>> getAllProjectConnections(
            @PathVariable("projectName") String projectName);

    /**
     * Retrieve a single project connection by identifier
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            connection identifier
     * @return a project connection
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{connectionId}")
    public ResponseEntity<Resource<ProjectConnection>> getProjectConnection(
            @PathVariable("projectName") String projectName, @PathVariable("connectionId") Long connectionId);

    /**
     * Create a new project connection
     *
     * @param projectName
     *            project name
     * @param pProjectConnection
     *            connection to create
     * @return the create project connection
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Resource<ProjectConnection>> createProjectConnection(
            @PathVariable("projectName") String projectName,
            @Valid @RequestBody final ProjectConnection pProjectConnection);

    /**
     * Update an existing project connection
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            project connection identifier
     * @param pProjectConnection
     *            project connection
     * @return updated connection
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{connectionId}")
    public ResponseEntity<Resource<ProjectConnection>> updateProjectConnection(
            @PathVariable("projectName") String projectName, @PathVariable("connectionId") Long connectionId,
            @Valid @RequestBody final ProjectConnection pProjectConnection);

    /**
     *
     * Delete an existing project connection
     *
     * @param projectName
     *            project name
     * @param connectionId
     *            project connection identifier
     * @return {@link Void}
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{connectionId}")
    public ResponseEntity<Void> deleteProjectConnection(@PathVariable("projectName") String projectName,
            @PathVariable("connectionId") Long connectionId);
}
