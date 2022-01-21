/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.authentication;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

@Component
@Primary
public class ProjectClientStub implements IProjectsClient {

    private static List<Project> projects = new ArrayList<>();

    private static long idCount = 0;

    @Override
    public ResponseEntity<PagedModel<EntityModel<Project>>> retrieveProjectList(final int pPage, final int pSize) {
        final PagedModel<EntityModel<Project>> page = new PagedModel<>(HateoasUtils.wrapList(projects),
                new PageMetadata(pSize, pPage, 1), new ArrayList<>());
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<Project>>> retrievePublicProjectList(int page, int size) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<Project>> createProject(final Project pNewProject) {
        pNewProject.setId(idCount++);
        projects.add(pNewProject);
        return new ResponseEntity<>(HateoasUtils.wrap(pNewProject), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<Project>> retrieveProject(final String pProjectName) {
        Project result = null;
        for (final Project project : projects) {
            if (project.getName().equals(pProjectName)) {
                result = project;
                break;
            }
        }
        return new ResponseEntity<>(HateoasUtils.wrap(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<Project>> updateProject(final String pProjectName,
            final Project pProjectToUpdate) {
        Project result = null;
        for (final Project project : projects) {
            if (project.getName().equals(pProjectName)) {
                project.setDescription(pProjectToUpdate.getDescription());
                result = project;
                break;
            }
        }
        return new ResponseEntity<>(HateoasUtils.wrap(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteProject(final String pProjectName) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
