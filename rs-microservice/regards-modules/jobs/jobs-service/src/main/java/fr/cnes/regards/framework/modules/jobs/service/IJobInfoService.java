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
package fr.cnes.regards.framework.modules.jobs.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * JobInfo service interface.
 * A Job is instanciate from a JobInfo (that can be viewed as a job description and job state) so sometimes we can speak
 * of jobs instead of jobs infos
 * @author oroussel
 * @author Léo Mieulet
 */
public interface IJobInfoService {

    /**
     * Find Job info with highest priority and update status to RUNNING
     */
    JobInfo findHighestPriorityQueuedJobAndSetAsToBeRun();

    /**
     * Create a JobInfo setting its state as PENDING ie <b>it will not be taken into account by job service until its
     * state is QUEUED</b>
     */
    JobInfo createAsPending(JobInfo jobInfo);

    /**
     * Create a JobInfo setting its state as QUEUED ie <b>it will be taken into account by job service as soon as
     * possible</b>
     */
    JobInfo createAsQueued(JobInfo jobInfo);

    JobInfo enqueueJobForId(UUID jobInfoId);

    /**
     * @param jobInfo the jobInfo to save
     * @return the updated jobInfo
     */
    JobInfo save(JobInfo jobInfo);

    /**
     * Lock job info, cannot be candidate for cleaning
     * @return the update job info
     */
    JobInfo lock(JobInfo jobInfo);

    /**
     * Unlock job info, make it candidate for cleaning
     * @return the update job info
     */
    JobInfo unlock(JobInfo jobInfo);

    /**
     * @return all jobs
     */
    List<JobInfo> retrieveJobs();

    /**
     * @return all jobs
     */
    Page<JobInfo> retrieveJobs(String className, Pageable page, JobStatus... statuses);

    /**
     * Retrieve all jobs with given state
     */
    List<JobInfo> retrieveJobs(JobStatus state);

    /**
     * Retrieve specified JObInfo
     * @param id JobInfo id
     */
    JobInfo retrieveJob(UUID id);

    /**
     * Ask for a job to be stopped (asynchronous method)
     * @param id job id
     */
    void stopJob(UUID id);

    /**
     * Update jobInfos completion ie percentCompleted and estimatedCompletion date
     */
    void updateJobInfosCompletion(Iterable<JobInfo> jobInfos);

    /**
     * On all tenants, remove out-of-date jobs ie :
     * - expired jobs
     * - terminated on success jobs several days ago
     * - terminated on error jobs several days ago
     */
    void cleanOutOfDateJobs();

    /**
     * On one tenant, remove out-of-date jobs ie :
     * - expired jobs
     * - terminated on success jobs several days ago
     * - terminated on error jobs several days ago
     */
    void cleanOutOfDateJobsOnTenant();

    /**
     * Return the number of job, for the current tenant, having provided statuses
     * @param className the class name of the job
     * @param statuses jobInfo statuses
     */
    Long retrieveJobsCount(String className, JobStatus... statuses);

    /**
     * @param jobInfo list of {@link JobInfo} to save
     */
    void saveAll(List<JobInfo> jobInfo);

    void updateJobInfosHeartbeat(Collection<UUID> ids);

    /**
     * Update dead jobs status in database with change their status from RUNNING to FAILED
     */
    void cleanDeadJobs();
}
