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
package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * JPA Repository for {@link SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
public interface ISnapshotProcessRepository extends JpaRepository<SnapshotProcess, Long> {

    Optional<SnapshotProcess> findBySource(String source);

    Set<SnapshotProcess> findBySourceIn(Collection<String> sources);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Set<SnapshotProcess> findByJobIdIn(List<UUID> jobIds);

    @Modifying
    @Query("UPDATE SnapshotProcess p SET p.jobId = null WHERE p.jobId IN (?1)")
    void removeTerminatedJobsById(List<UUID> jobIds);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<SnapshotProcess> findByJobIdIsNull(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<SnapshotProcess> findByJobIdIsNotNullOrderByLastUpdateDateAsc(Pageable pageable);

    @Modifying
    @Query("DELETE FROM SnapshotProcess p where p.source NOT IN (SELECT s.source FROM SessionStep s) "
           + "AND (p.lastUpdateDate IS NULL OR p.lastUpdateDate <= ?1)")
    int deleteUnusedProcess(OffsetDateTime limitDate);
}