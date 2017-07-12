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

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.EventType;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameters;
import fr.cnes.regards.framework.modules.jobs.domain.Output;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

/**
 *
 */
public class AJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(AJob.class);

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            if (!Thread.currentThread().isInterrupted()) {
                LOG.info("AJob: Waiting..");
                try {
                    sendEvent(EventType.JOB_PERCENT_COMPLETED, i * 10);
                    Thread.sleep(800);
                } catch (final InterruptedException e) {
                    LOG.warn("Thread interrupted, closing", e);
                    return;
                }
            } else {
                LOG.warn("Thread interrupted, closing");
                return;
            }
        }
        try {
            sendEvent(EventType.SUCCEEDED);
        } catch (final InterruptedException e) {
            LOG.error("Failed to send success to parent thread", e);
        }
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Output> getResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StatusInfo getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasResult() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needWorkspace() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setWorkspace(final Path pPath) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setParameters(JobParameters pParameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        parameters = pParameters;

    }

}
