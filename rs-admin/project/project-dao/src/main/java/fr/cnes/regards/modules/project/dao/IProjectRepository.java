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
package fr.cnes.regards.modules.project.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class IProjectRepository
 *
 * JPA Repository to access Project entities
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@InstanceEntity
public interface IProjectRepository extends JpaRepository<Project, Long> {

    Project findOneByNameIgnoreCase(String pName);

    Page<Project> findByIsPublicTrue(Pageable pPageable);

    /**
     *
     * Retrieve all active project (not deleted)
     *
     * @return active {@link Project}s
     * @since 1.0-SNAPSHOT
     */
    List<Project> findByIsDeletedFalse();

    /**
     * Check if a project exists and is not deleted
     * @param id project identifier
     * @return true if it's active
     */
    default boolean isActiveProject(Long id) {
        Project one = findOne(id);
        return (one != null) && !one.isDeleted();
    }
}
