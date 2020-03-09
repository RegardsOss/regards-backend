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
package fr.cnes.regards.modules.project.client.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Default project client
 * @author Marc SORDI
 *
 */
@Profile("standalone")
@Component
public class DefaultProjectClient implements IProjectsClient {

    /**
     * List of configurated tenants
     */
    @Value("${regards.tenant.host}")
    private String host;

    @Override
    public ResponseEntity<PagedResources<Resource<Project>>> retrieveProjectList(int pPage, int pSize) {
        throw new UnsupportedOperationException("Not implemented in default project client");
    }

    @Override
    public ResponseEntity<PagedResources<Resource<Project>>> retrievePublicProjectList(int page, int size) {
        throw new UnsupportedOperationException("Not implemented in default project client");
    }

    @Override
    public ResponseEntity<Resource<Project>> createProject(@Valid Project pNewProject) {
        throw new UnsupportedOperationException("Not implemented in default project client");
    }

    @Override
    public ResponseEntity<Resource<Project>> retrieveProject(String pProjectName) {
        Project project = new Project("desc", null, true, pProjectName);
        project.setHost(host);
        return ResponseEntity.ok(new Resource<>(project));
    }

    @Override
    public ResponseEntity<Resource<Project>> updateProject(String pProjectName, Project pProjectToUpdate) {
        throw new UnsupportedOperationException("Not implemented in default project client");
    }

    @Override
    public ResponseEntity<Void> deleteProject(String pProjectName) {
        throw new UnsupportedOperationException("Not implemented in default project client");
    }

}
