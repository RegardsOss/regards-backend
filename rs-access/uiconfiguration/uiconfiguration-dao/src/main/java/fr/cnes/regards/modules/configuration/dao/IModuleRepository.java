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
package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
public interface IModuleRepository extends JpaRepository<Module, Long> {

    /**
     *
     * Retrieve modules for the given application id.
     *
     * @param pApplicationId
     * @return Page of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> findByApplicationId(String pApplicationId, Pageable pPageable);

    /**
     * Retrieve all modules for the given application id
     *
     * @param pApplicationId
     * @return List of {@link Module}
     * @since 1.0-SNAPSHOT
     */
    List<Module> findByApplicationId(String pApplicationId);

    /**
     *
     * Retrieve modules for the given application id without pagination
     *
     * @param pApplicationId
     * @return
     * @since 1.0-SNAPSHOT
     */
    List<Module> findByApplicationIdAndPageHomeTrue(String pApplicationId);

    /**
     *
     * Retrieve modules for the given application id.
     *
     * @param pApplicationId
     * @return {@link Module}
     * @since 1.0-SNAPSHOT
     */
    Page<Module> findByApplicationIdAndActiveTrue(String pApplicationId, Pageable pPageable);

}
