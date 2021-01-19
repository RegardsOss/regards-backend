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
package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.cnes.regards.modules.configuration.domain.Module;

/**
 *
 * Class IModuleRepository
 *
 * JPA Repository for Module entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IModuleRepository extends JpaRepository<Module, Long>, JpaSpecificationExecutor<Module> {

    /**
     *
     * Retrieve modules for the given application id.
     *
     * @param applicationId
     * @param pageable
     * @return Page of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> findByApplicationId(String applicationId, Pageable pageable);

    /**
     * Retrieve all modules for the given application id
     *
     * @param applicationId
     * @return List of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    List<Module> findByApplicationId(String applicationId);

    /**
     *
     * Retrieve modules for the given application id without pagination
     *
     * @param applicationId
     * @return {@link Module}s
     * @since 1.0-SNAPSHOT
     */
    List<Module> findByApplicationIdAndPageHomeTrue(String applicationId);

    /**
     *
     * Retrieve modules for the given application id.
     *
     * @param applicationId
     * @param pageable
     * @return {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> findByApplicationIdAndActiveTrue(String applicationId, Pageable pageable);

}
