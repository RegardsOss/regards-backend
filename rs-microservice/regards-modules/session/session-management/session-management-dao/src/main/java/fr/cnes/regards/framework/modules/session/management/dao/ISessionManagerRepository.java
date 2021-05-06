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
package fr.cnes.regards.framework.modules.session.management.dao;

import fr.cnes.regards.framework.modules.session.management.domain.Session;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Session}
 * @author Iliana Ghazali
 */
@Repository
public interface ISessionManagerRepository extends JpaRepository<Session, Long>, JpaSpecificationExecutor<Session> {

    Optional<Session> findBySourceAndName(String source, String sessionName);

    long countBySourceAndNameIn(String name, Set<String> collect);

    long countBySource(String sourceName);

    Page<Session> findByLastUpdateDateBefore(OffsetDateTime startClean, Pageable pageable);
}
