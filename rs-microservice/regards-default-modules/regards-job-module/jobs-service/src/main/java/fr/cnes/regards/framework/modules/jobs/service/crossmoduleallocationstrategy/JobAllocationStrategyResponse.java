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
package fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

/**
 * Response from JobAllocationStrategy plugin
 * 
 * @author Léo Mieulet
 */
public class JobAllocationStrategyResponse {

    /**
     * The tenant name that there is a remaining slot, or null
     */
    private final String projectName;

    /**
     * the list of jobQueue
     */
    private final List<IJobQueue> jobQueueList;

    /**
     * @param pProjectNameToPull
     *            the tenant with a remaining slot
     * @param pJobQueueList
     *            the new queue list
     */
    public JobAllocationStrategyResponse(final String pProjectNameToPull, final List<IJobQueue> pJobQueueList) {
        projectName = pProjectNameToPull;
        jobQueueList = pJobQueueList;
    }

    /**
     * @return the projectName that the JobPuller will use to retrieve a job to execute
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @return the jobQueueList
     */
    public List<IJobQueue> getJobQueueList() {
        return jobQueueList;
    }
}
