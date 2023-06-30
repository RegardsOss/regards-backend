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
package fr.cnes.regards.modules.dam.dao.dataaccess;

import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository handling AccessGroup
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Repository
public interface IAccessGroupRepository extends JpaRepository<AccessGroup, Long> {

    /**
     * find an access group by its name
     *
     * @return the access group or null if none found
     */
    AccessGroup findOneByName(String name);

    Optional<AccessGroup> findByName(String name);

    @Override
    List<AccessGroup> findAllById(Iterable<Long> ids);

    /**
     * Find all public or non public group
     *
     * @param isPublic whether we have to select public or non public groups
     * @param pageable {@link Pageable}
     * @return list of public or non public groups
     */
    Page<AccessGroup> findByIsPublic(boolean isPublic, Pageable pageable);

}
