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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

/**
 * @author Léo Mieulet
 */
public abstract class AbstractJob implements IJob {

    /**
     * Share a queue between the job and the JobHandler
     */
    private BlockingQueue<IEvent> queueEvent;

    /**
     * JobInfo id
     */
    private Long jobInfoId;

    /**
     * Store the tenantName
     */
    private String tenantName;

    /**
     * Job parameters
     */
    protected JobParameters parameters;

    private Path workspace;

    @Override
    public void setQueueEvent(final BlockingQueue<IEvent> pQueueEvent) {
        queueEvent = pQueueEvent;
    }

    /**
     * Send an event to the JobHandler
     *
     * @param pEventType
     *            the event type
     * @param pValue
     *            data related to the event
     * @throws InterruptedException
     *             If interrupted while waiting
     */
    protected void sendEvent(final EventType pEventType, final Object pValue) throws InterruptedException {
        queueEvent.put(new Event(pEventType, pValue, jobInfoId, tenantName));
    }

    /**
     * @param pEventType
     *            the event type
     * @throws InterruptedException
     *             If interrupted while waiting
     */
    protected void sendEvent(final EventType pEventType) throws InterruptedException {
        queueEvent.put(new Event(pEventType, null, jobInfoId, tenantName));
    }

    /**
     * When the JobHandler creates this job, it saves the jobId
     *
     * @param pJobInfoId
     */
    @Override
    public void setJobInfoId(final Long pJobInfoId) {
        jobInfoId = pJobInfoId;
    }

    /**
     * @return the parameters
     */
    public JobParameters getParameters() {
        return parameters;
    }

    /**
     * @param pTenantName
     *            the tenantName to set
     */
    @Override
    public void setTenantName(final String pTenantName) {
        tenantName = pTenantName;
    }

    @Override
    public void setWorkspace(Path pWorkspace) {
        workspace = pWorkspace;
    }

    public Path getWorkspace() {
        return workspace;
    }
}
