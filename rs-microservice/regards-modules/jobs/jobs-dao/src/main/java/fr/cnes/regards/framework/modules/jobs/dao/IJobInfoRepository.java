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
package fr.cnes.regards.framework.modules.jobs.dao;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Interface for a JPA auto-generated CRUD repository managing Jobs.
 *
 * @author Olivier Rousselot
 */
public interface IJobInfoRepository extends CrudRepository<JobInfo, UUID> {

    /**
     * "The lock acquisition request skips the already locked rows.
     * It uses a SELECT …​ FOR UPDATE SKIP LOCKED in PostgreSQL 9.5
     */
    String UPGRADE_SKIPLOCKED = "-2";

    /**
     * @param status the {@link JobStatus} to used for the request
     * @return a list of {@link JobInfo}
     */
    List<JobInfo> findAllByStatusStatus(JobStatus status);

    // Do not use entity graph it makes max computation into memory
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = UPGRADE_SKIPLOCKED) })
    JobInfo findFirstByStatusStatusOrderByPriorityDesc(JobStatus status);

    default JobInfo findHighestPriorityQueued() {
        return findFirstByStatusStatusOrderByPriorityDesc(JobStatus.QUEUED);
    }

    @EntityGraph(attributePaths = { "parameters" }, type = EntityGraph.EntityGraphType.LOAD)
    JobInfo findCompleteById(UUID id);

    @Modifying
    @Query("update JobInfo j set j.lastHeartbeatDate = :heartbeatDate where j.id in :ids")
    void updateHeartbeatDateForIdsIn(@Param("heartbeatDate") OffsetDateTime now,
                                     @Param("ids") Collection<UUID> collect);

    @Modifying
    @Query(
        "update JobInfo j set j.status.percentCompleted = :completion, j.status.estimatedCompletion = :estimationCompletionDate, "
        + "j.lastCompletionUpdate = :updateCompletionDate where j.id = :id and j.status.status = 'RUNNING'")
    void updateCompletion(@Param("completion") int percentCompleted,
                          @Param("estimationCompletionDate") OffsetDateTime estimatedCompletion,
                          @Param("id") UUID id,
                          @Param("updateCompletionDate") OffsetDateTime updateDate);

    @Modifying
    @Query("update JobInfo jobInfo set jobInfo.expirationDate = :expirationDate where jobInfo.id in :jobInfoIds")
    void updateExpirationDate(@Param("expirationDate") OffsetDateTime expirationDate,
                              @Param("jobInfoIds") Set<UUID> jobInfoIds);

    /**
     * Count the number of jobs with provided statuses
     */
    Long countByClassNameAndStatusStatusIn(String className, JobStatus... statuses);

    Long countByStatusStatusIn(JobStatus... statuses);

    @MultitenantTransactional
    default void deleteExpiredJobs() {
        deleteExpiredJobs(OffsetDateTime.now(),
                          List.of(JobStatus.QUEUED.name(), JobStatus.TO_BE_RUN.name(), JobStatus.RUNNING.name()));
    }

    /**
     * Delete all succeeded jobs finished since a given number of days ago.
     */
    @MultitenantTransactional
    default void deleteSucceededJobsSince(int days) {
        deleteFinishedJobsByStatusAndStopDateLessThan(OffsetDateTime.now().minusDays(days),
                                                      List.of(JobStatus.SUCCEEDED.name()));
    }

    /**
     * Delete all failed or aborted jobs finished since a given number of days ago.
     */
    @MultitenantTransactional
    default void deleteFailedAndAbortJobsSince(int days) {
        deleteFinishedJobsByStatusAndStopDateLessThan(OffsetDateTime.now().minusDays(days),
                                                      List.of(JobStatus.FAILED.name(), JobStatus.ABORTED.name()));
    }

    /**
     * Delete all jobs by given statuses and stop date
     */
    @Modifying
    @Query(value = "DELETE FROM {h-schema}t_job_info WHERE locked = false AND stop_date <= :stopDate AND "
                   + "status "
                   + "IN (:statuses) ", nativeQuery = true)
    @MultitenantTransactional
    void deleteFinishedJobsByStatusAndStopDateLessThan(@Param("stopDate") OffsetDateTime stopDate,
                                                       @Param("statuses") List<String> statuses);

    /**
     * Delete all expired jobs with given statuses
     */
    @Modifying
    @Query(value = "DELETE FROM {h-schema}t_job_info WHERE locked = false AND expire_date <= :expirationDate AND "
                   + "status IN "
                   + "(:statuses)", nativeQuery = true)
    @MultitenantTransactional
    void deleteExpiredJobs(@Param("expirationDate") OffsetDateTime expirationDate,
                           @Param("statuses") List<String> statuses);

    /**
     * Search jobs with given status at given date (only unlocked)
     */
    @EntityGraph(attributePaths = { "parameters" }, type = EntityGraph.EntityGraphType.LOAD)
    List<JobInfo> findByStatusStopDateLessThanAndLockedAndStatusStatusIn(OffsetDateTime stopDate,
                                                                         Boolean locked,
                                                                         JobStatus... statuses);

    /**
     * Search succeeded jobs since given number of days
     */
    default List<JobInfo> findSucceededJobsSince(int days) {
        return findByStatusStopDateLessThanAndLockedAndStatusStatusIn(OffsetDateTime.now().minusDays(days),
                                                                      false,
                                                                      JobStatus.SUCCEEDED);
    }

    /**
     * Search failed and aborted jobs since given number of days
     */
    default List<JobInfo> findFailedOrAbortedJobsSince(int days) {
        return findByStatusStopDateLessThanAndLockedAndStatusStatusIn(OffsetDateTime.now().minusDays(days),
                                                                      false,
                                                                      JobStatus.FAILED,
                                                                      JobStatus.ABORTED);
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
        return countByOwnerAndStatusStatusIn(user,
                                             JobStatus.PENDING,
                                             JobStatus.QUEUED,
                                             JobStatus.TO_BE_RUN,
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
    List<JobInfo> findByOwnerAndStatusStatusAndClassNameOrderByPriorityDesc(String owner,
                                                                            JobStatus status,
                                                                            String className,
                                                                            Pageable page);

    /**
     * Find top priority user pending jobs
     *
     * @param count number of results to retrieve
     */
    default List<JobInfo> findTopUserPendingJobs(String user, String className, int count) {
        return findByOwnerAndStatusStatusAndClassNameOrderByPriorityDesc(user,
                                                                         JobStatus.PENDING,
                                                                         className,
                                                                         PageRequest.of(0, count));
    }

    /**
     * Find jobs with a trigger date expired
     */
    List<JobInfo> findByStatusStatusAndTriggerAfterDateLessThan(JobStatus status,
                                                                OffsetDateTime currentDateTime,
                                                                Pageable page);

    Long countByClassNameAndParameters_NameAndParameters_ValueAndStatusStatusIn(String className,
                                                                                String parameterName,
                                                                                String parameterValue,
                                                                                JobStatus... jobStatuses);

    Page<JobInfo> findByClassNameAndStatusStatusIn(String className, JobStatus[] statuses, Pageable page);

    Collection<JobInfo> findAllByClassNameAndParametersValueContaining(String className, String partOfParameterValue);
}
