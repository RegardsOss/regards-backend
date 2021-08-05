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
package fr.cnes.regards.modules.dam.dao.dataaccess;

import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository handling AccessGroup
 *
 * @author Sylvain Vissiere-Guerinet
 */
public interface IAccessGroupRepository extends JpaRepository<AccessGroup, Long> {

    /**
     * find an access group by its name
     * @param pName
     * @return the access group or null if none found
     */
    AccessGroup findOneByName(String pName);

    Optional<AccessGroup> findByName(String name);

    @Override
    List<AccessGroup> findAllById(Iterable<Long> longs);

    /**
     * Find all public or non public group
     * @param isPublic whether we have to select public or non public groups
     * @param pageable {@link Pageable}
     * @return list of public or non public groups
     */
    default Page<AccessGroup> findAllByIsPublic(Boolean isPublic, Pageable pageable) {
        Page<BigInteger> idPage = findIdPageByIsPublic(isPublic, pageable);
        List<AccessGroup> accessGroups = findAllById(idPage.getContent().stream().map(BigInteger::longValue).collect(Collectors.toList()));
        return new PageImpl<>(accessGroups, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query(value = "SELECT ag.id FROM {h-schema}t_access_group ag LEFT JOIN {h-schema}ta_access_group_users agu on ag.id=agu.access_group_id WHERE ag.public=:isPublic",
            nativeQuery = true)
    Page<BigInteger> findIdPageByIsPublic(@Param("isPublic") Boolean isPublic, Pageable pageable);

    @Override
    @EntityGraph(value = "graph.accessgroup.users")
    default Page<AccessGroup> findAll(Pageable pageable) {
        Page<Long> idPage = findIdPage(pageable);
        List<AccessGroup> accessGroups = findAllById(idPage.getContent());
        return new PageImpl<>(accessGroups, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select ag.id from AccessGroup ag")
    Page<Long> findIdPage(Pageable pageable);

}
