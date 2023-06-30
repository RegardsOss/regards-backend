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
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.AccessRight;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public interface IAccessRightRepository extends JpaRepository<AccessRight, Long> {

    /**
     * Retrieve an AccessRight with the associated Dataset and AccessGroup.
     *
     * @param pId the {@link AccessRight} to retrieve
     * @return {@link AccessRight} with {@link Dataset} associated.
     * @since 1.0-SNAPSHOT
     */
    @Override
    @EntityGraph(value = "graph.accessright.dataset.and.accessgroup", type = EntityGraph.EntityGraphType.LOAD)
    Optional<AccessRight> findById(Long id);

    default Page<AccessRight> findAllByDataset(Dataset dataset, Pageable pageable) {
        Page<Long> idPage = findIdPageByDataset(dataset, pageable);
        List<AccessRight> accessRights = findAllById(idPage.getContent());
        return new PageImpl<>(accessRights, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select ar.id from AccessRight ar where ar.dataset=:dataset")
    Page<Long> findIdPageByDataset(@Param("dataset") Dataset dataset, Pageable pageable);

    @Override
    default Page<AccessRight> findAll(Pageable pageable) {
        Page<Long> idPage = findIdPage(pageable);
        List<AccessRight> accessRights = findAllById(idPage.getContent());
        return new PageImpl<>(accessRights, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select ar.id from AccessRight ar")
    Page<Long> findIdPage(Pageable pageable);

    default Page<AccessRight> findAllByAccessGroup(AccessGroup accessGroup, Pageable pageable) {
        Page<Long> idPage = findIdPageByAccessGroup(accessGroup, pageable);
        List<AccessRight> accessRights = findAllById(idPage.getContent());
        return new PageImpl<>(accessRights, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select ar.id from AccessRight ar where ar.accessGroup=:accessGroup")
    Page<Long> findIdPageByAccessGroup(@Param("accessGroup") AccessGroup accessGroup, Pageable pageable);

    @Override
    @EntityGraph(value = "graph.accessright.dataset.and.accessgroup", type = EntityGraph.EntityGraphType.LOAD)
    List<AccessRight> findAllById(Iterable<Long> ids);

    @EntityGraph(value = "graph.accessright.dataset.and.accessgroup", type = EntityGraph.EntityGraphType.LOAD)
    List<AccessRight> findAllByDataset(Dataset dataset);

    /**
     * This method returns zero or one AccessRight
     *
     * @return {@link AccessRight}s by page
     */
    default Page<AccessRight> findAllByAccessGroupAndDataset(AccessGroup accessGroup,
                                                             Dataset dataset,
                                                             Pageable pageable) {
        Page<Long> idPage = findIdPageByAccessGroupAndDataset(accessGroup, dataset, pageable);
        List<AccessRight> accessRights = findAllById(idPage.getContent());
        return new PageImpl<>(accessRights, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select ar.id from AccessRight ar where ar.accessGroup=:accessGroup and ar.dataset=:dataset")
    Page<Long> findIdPageByAccessGroupAndDataset(@Param("accessGroup") AccessGroup accessGroup,
                                                 @Param("dataset") Dataset dataset,
                                                 Pageable pageable);

    /**
     * This methods return only zero or one AccessRight
     *
     * @return {@link AccessRight}
     */
    @EntityGraph(value = "graph.accessright.plugins", type = EntityGraph.EntityGraphType.LOAD)
    Optional<AccessRight> findAccessRightByAccessGroupAndDataset(AccessGroup accessGroup, Dataset dataset);

    /**
     * Find all {@link AccessRight}s associated a dataAccess plugin.
     *
     * @return {@link AccessRight}s
     */
    @EntityGraph(value = "graph.accessright.dataset.and.accessgroup", type = EntityGraph.EntityGraphType.LOAD)
    List<AccessRight> findByDataAccessPluginNotNull();

}
