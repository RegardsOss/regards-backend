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
package fr.cnes.regards.modules.fileaccess.dao;

import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA Repository to handle access to {@link StorageLocationConfiguration} entities.
 *
 * @author Thibaud Michaudel
 */
public interface IStorageLocationConfigurationRepository extends JpaRepository<StorageLocationConfiguration, Long> {

    Optional<StorageLocationConfiguration> findByName(String name);

    boolean existsByName(String name);

}
