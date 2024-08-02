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
package fr.cnes.regards.modules.project.dao;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Class IProjectConnectionRepository
 * <p>
 * JPA Repository to access ProjectConnection entities.
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 */
@InstanceEntity
public interface IProjectConnectionRepository extends JpaRepository<ProjectConnection, Long> {

    /**
     * Retrieve all tenant connections for a specified microservice
     *
     * @param microservice microservice name
     * @return all microservice connections
     */
    List<ProjectConnection> findByMicroserviceAndProjectIsDeletedFalse(String microservice);

    /**
     * Retrieve all enabled tenant connections for a specified microservice
     *
     * @param microservice microservice name
     * @return all enabled microservice connections
     */
    List<ProjectConnection> findByMicroserviceAndStateAndProjectIsDeletedFalse(String microservice,
                                                                               TenantConnectionState state);

    /**
     * Retrieve all connections with provided URL
     *
     * @param url database connection url
     * @return list of connections
     */
    List<ProjectConnection> findByUrl(String url);

    /**
     * Retrieve connection for project and microservice
     *
     * @param projectName  project name
     * @param microservice microservice
     * @return {@link ProjectConnection}
     */
    ProjectConnection findOneByProjectNameAndMicroservice(final String projectName, final String microservice);

    /**
     * Find all {@link ProjectConnection}s whose project has given <code>name</code>.<br>
     * Custom query auto-implemented by JPA thanks to the method naming convention.
     *
     * @param projectName the project name
     * @return A {@link Page} of found {@link ProjectConnection}s
     */
    Page<ProjectConnection> findByProjectName(String projectName, Pageable pageable);

    /**
     * Delete all project connections
     */
    List<ProjectConnection> deleteByProjectId(Long projectId);

    /**
     * List all active connections for specified microservice. Connections from deleted projects are rejected.
     *
     * @param microservice microservice
     * @return list of {@link ProjectConnection}
     */
    default List<ProjectConnection> getMicroserviceConnections(String microservice) {
        return findByMicroserviceAndStateAndProjectIsDeletedFalse(microservice, TenantConnectionState.ENABLED);
    }

    /**
     * Check if project connection exists
     *
     * @param projectName  project name
     * @param microservice microservice
     * @return true if connection exists
     */
    default boolean existsProjectConnection(String projectName, String microservice) {
        return findOneByProjectNameAndMicroservice(projectName, microservice) != null;
    }
}
