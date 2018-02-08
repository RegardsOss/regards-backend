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

import javax.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * Interface for a JPA auto-generated CRUD repository managing Jobs.
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
    @Lock(LockModeType.PESSIMISTIC_READ)
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

    Long countByStatusStatusIn(JobStatus... statuses);

    /**
     * Delete jobs expired at given date (only unlocked)
     */
    @Modifying
    @Query("delete JobInfo j where j.expirationDate < ?1 and j.status.status not in ('QUEUED', 'TO_BE_RUN', 'RUNNING') "
            + "and j.locked = FALSE")
    void deleteAtDateExpiredJobs(OffsetDateTime date);

    /**
     * Delete currently expired jobs
     */
    default void deleteExpiredJobs() {
        deleteAtDateExpiredJobs(OffsetDateTime.now());
    }

    /**
     * Delete jobs with given status at given date (only unlocked)
     */
    @Modifying
    @Query("delete JobInfo j where j.status.stopDate < ?1 and j.status.status in ?2 and j.locked = FALSE")
    void deleteWithStatusAtDateJobs(OffsetDateTime date, JobStatus... statuses);

    /**
     * Delete succeeded jobs since given number of days
     */
    default void deleteSucceededJobsSince(int days) {
        deleteWithStatusAtDateJobs(OffsetDateTime.now().minusDays((long)days), JobStatus.SUCCEEDED);
    }

    /**
     * Delete failed and aborted jobs since given number of days
     */
    default void deleteFailedOrAbortedJobsSince(int days) {
        deleteWithStatusAtDateJobs(OffsetDateTime.now().minusDays((long)days), JobStatus.FAILED, JobStatus.ABORTED);
    }



    /**
     * Count all jobs that will be launched in the future and jobs that are currently running
     */
    default long countFutureAndRunningJobs() {
        return countByStatusStatusIn(JobStatus.PENDING, JobStatus.QUEUED, JobStatus.TO_BE_RUN, JobStatus.RUNNING);
    }

    Long countByOwnerAndStatusStatusIn(String owner, JobStatus... statuses);

    /**
     * Count user jobs that will be launched in the future and jobs that are currently running
     */
    default long countUserFutureAndRunningJobs(String user) {
        return countByOwnerAndStatusStatusIn(user, JobStatus.PENDING, JobStatus.QUEUED, JobStatus.TO_BE_RUN,
                                             JobStatus.RUNNING);
    }

    /**
     * Count jobs that are planned to be launched and jobs that are currently running
     */
    default long countUserPlannedAndRunningJobs(String user) {
        return countByOwnerAndStatusStatusIn(user, JobStatus.QUEUED, JobStatus.TO_BE_RUN, JobStatus.RUNNING);
    }

    /**
     * Find all jobInfo by owner with a specified status. Results are ordered by desc priority and limited thanks to
     * page
     */
    List<JobInfo> findByOwnerAndStatusStatusOrderByPriorityDesc(String owner, JobStatus status, Pageable page);

    /**
     * Find top priority user pending jobs
     * @param count number of results to retrieve
     */
    default List<JobInfo> findTopUserPendingJobs(String user, int count) {
        return findByOwnerAndStatusStatusOrderByPriorityDesc(user, JobStatus.PENDING, new PageRequest(0, count));
    }
}
