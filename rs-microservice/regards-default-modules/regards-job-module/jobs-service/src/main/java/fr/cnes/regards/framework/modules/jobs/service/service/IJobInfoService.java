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
package fr.cnes.regards.framework.modules.jobs.service.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * @author Léo Mieulet
 */
public interface IJobInfoService {

    /**
     * Store the JobInfo into the database, and publish it on the broker message
     *
     *
     * @param pJobInfo
     *            Store the new jobInfo in the database
     * @return the status of the new jobInfo
     */
    JobInfo createJobInfo(JobInfo pJobInfo);

    /**
     * @return returns all jobs
     */
    List<JobInfo> retrieveJobInfoList();

    /**
     * @param pState
     *            the state filter
     * @return the list of jobs matching that the provided state
     */
    List<JobInfo> retrieveJobInfoListByState(JobStatus pState);

    /**
     * @param pJobInfoId
     *            the jobInfo id
     * @return the corresponding jobInfo
     * @throws EntityNotFoundException
     *             The job does not exist
     */
    JobInfo retrieveJobInfoById(Long pJobInfoId) throws EntityNotFoundException;

    /**
     * @param pJobInfo
     *            the jobInfo to save
     * @return the updated jobInfo
     */
    JobInfo save(JobInfo pJobInfo);

    /**
     * @param pJobInfoId
     *            the jobInfo id
     * @return the updated jobInfo
     * @throws EntityNotFoundException
     *             The job does not exist
     */
    JobInfo stopJob(Long pJobInfoId) throws EntityNotFoundException;
}
