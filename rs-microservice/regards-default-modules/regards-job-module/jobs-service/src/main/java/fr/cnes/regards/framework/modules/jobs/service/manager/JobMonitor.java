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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.IEvent;

/**
 * A thread between JobHandler and Jobs that receives events and send them back to JobHandler
 * 
 * @author Léo Mieulet
 */
public class JobMonitor implements Runnable {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobMonitor.class);

    /**
     * Thread safe queue
     */
    private final BlockingQueue<IEvent> queueEvent;

    /**
     * Store jobHandler
     */
    private final IJobHandler jobHandler;

    /**
     * Stop the jobMonitor on shutdown
     */
    private boolean isRunning;

    /**
     * @param pJobHandler
     *            a {link {@link JobHandler}}
     *
     */
    public JobMonitor(final JobHandler pJobHandler) {
        super();
        queueEvent = new LinkedBlockingDeque<>();
        jobHandler = pJobHandler;
        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                final IEvent event = queueEvent.take();
                jobHandler.onEvent(event);
            } catch (final InterruptedException e) {
                LOG.warn("JobMonitor interrupted, closing");
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

    /**
     * @return the queueEvent
     */
    public BlockingQueue<IEvent> getQueueEvent() {
        return queueEvent;
    }

}
