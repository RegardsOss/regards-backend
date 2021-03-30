/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.toponyms.dao;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Sébastien Binda
 *
 */
@InstanceEntity
public interface IToponymsRepository extends JpaRepository<Toponym, String>, JpaSpecificationExecutor<Toponym> {

    Page<Toponym> findByLabelFrContainingIgnoreCaseAndVisible(String partialLabel, boolean visible, Pageable page);

    Page<Toponym> findByLabelContainingIgnoreCaseAndVisible(String partialLabel, boolean visible, Pageable page);

    @Query(value = "select bid, label, label_fr, ST_Simplify(geom, ?2,true) as geom, copyright, description, visible,"
            + "creation_date, last_access_date, project, author from {h-schema}t_toponyms where bid = ?1",
            nativeQuery = true)
    Optional<Toponym> findOneSimplified(String businessId, double tolerance);

    Page<Toponym> findByVisible(boolean visible, Pageable page);

    Page<Toponym> findByVisibleAndExpirationDateBefore(boolean visible, OffsetDateTime expirationDate, Pageable page);

    int countByToponymMetadataAuthorAndToponymMetadataCreationDateBetween(String user, OffsetDateTime startDate, OffsetDateTime endDate);

    @Transactional
    void deleteByVisible(boolean visibility);
}
