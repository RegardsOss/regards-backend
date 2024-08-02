/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.project.service;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.project.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Class IProjectService
 * <p>
 * Interface for ProjectService. Allow to query projects entities.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 * @author SébastienBinda
 */
public interface IProjectService {

    /**
     * Retrieve a project with is unique name
     *
     * @param pProjectName project name to retrieve
     * @return Project
     * @throws ModuleException Thrown when no {@link Project} with passed <code>name</code> exists.
     */
    Project retrieveProject(String pProjectName) throws ModuleException;

    /**
     * Delete a project
     *
     * @param pProjectName Project name to delete
     * @throws ModuleException Thrown when no {@link Project} with passed <code>name</code> exists.
     */
    void deleteProject(String pProjectName) throws ModuleException;

    /**
     * Update a project
     *
     * @param pProjectName Project name to update
     * @param pProject     Project to update
     * @return Updated Project
     * @throws ModuleException <br/>
     *                         {@link EntityNotFoundException}</b> if the request project does not exists.<br/>
     *                         {@link EntityInvalidException} if pProjectName doesn't match the given project
     */
    Project updateProject(String pProjectName, Project pProject) throws ModuleException;

    /**
     * Retrieve project List.
     *
     * @return List of projects
     */
    List<Project> retrieveProjectList();

    /**
     * Retrieve project List.
     *
     * @param pPageable pagination informations
     * @return List of projects
     */
    Page<Project> retrieveProjectList(Pageable pPageable);

    /**
     * Retrieve all public projects
     *
     * @param pPageable pagination informations
     * @return List of public projects
     */
    Page<Project> retrievePublicProjectList(Pageable pPageable);

    /**
     * Create a new project
     *
     * @param pNewProject Project ot create
     * @return Created project
     * @throws ModuleException <br/>
     *                         {@link EntityException} If Project already exists for the given name
     */
    Project createProject(Project pNewProject) throws ModuleException;

}
