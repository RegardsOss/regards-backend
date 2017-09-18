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
package fr.cnes.regards.framework.modules.jobs.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * Interface for a JPA auto-generated CRUD repository managing Jobs.
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
public interface IJobInfoRepository extends CrudRepository<JobInfo, UUID> {
    /**
     * @param status the {@link JobStatus} to used for the request
     * @return a list of {@link JobInfo}
     */
    List<JobInfo> findAllByStatusStatus(JobStatus status);

    // Do not use entity graph it makes max computation into memory
    JobInfo findFirstByStatusStatusOrderByPriorityDesc(JobStatus status);

    default JobInfo findHighestPriorityQueued() {
        return findFirstByStatusStatusOrderByPriorityDesc(JobStatus.QUEUED);
    }

    @EntityGraph(attributePaths = { "parameters" })
    JobInfo findById(UUID id);

    @Modifying
    @Query("update JobInfo j set j.status.percentCompleted = ?1, j.status.estimatedCompletion = ?2 where j.id = ?3 "
            + "and j.status.status = 'RUNNING'")
    void updateCompletion(int percentCompleted, OffsetDateTime estimatedCompletion, UUID id);
}
