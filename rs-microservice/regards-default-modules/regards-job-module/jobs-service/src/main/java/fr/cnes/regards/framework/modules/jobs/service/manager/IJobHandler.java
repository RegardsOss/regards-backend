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
package fr.cnes.regards.framework.modules.jobs.service.manager;

import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.IEvent;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;

/**
 * Provide a job pool
 * 
 * @author Léo Mieulet
 */
public interface IJobHandler {

    /**
     * Stop the thread pool in the next hours
     *
     */
    void shutdown();

    /**
     * Stop the thread pool in few seconds
     *
     */
    void shutdownNow();

    /**
     * Delete a job: Ensure that the job will be interrupted if it was running and change its status to Aborted
     *
     * @param pJobInfoId
     *            abort the corresponding pJobInfo id
     * @return the updated status of that job
     */
    StatusInfo abort(final Long pJobInfoId);

    /**
     * Retrieve the jobInfo, then execute that job.
     *
     * @param pTenantName
     *            the project that the job belong
     * @param pJobInfoId
     *            the jobInfo id we are running
     * @return the updated status of the jobInfo status
     */
    StatusInfo execute(String pTenantName, Long pJobInfoId);

    /**
     * Receive event from jobs (throw JobMonitor)
     *
     * @param pEvent
     *            running jobs send events to JobHandler
     */
    void onEvent(IEvent pEvent);

    /**
     * @return the number of concurrent jobs allowed on the same time
     */
    Integer getMaxJobCapacity();

    /**
     * @return a map with as key the tenant name and the current number of active threads
     */
    Map<String, Integer> getNbActiveThreadByTenant();

    /**
     * @return true if the microservice cannot accept more jobs
     */
    boolean isThreadPoolFull();

}
