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
package fr.cnes.regards.framework.modules.jobs.service.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.framework.modules.jobs.service.manager.IJobHandler;

/**
 * @author Léo Mieulet
 */
public class StoppingJobSubscriber implements IHandler<StoppingJobEvent> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StoppingJobSubscriber.class);

    /**
     * the job handler
     */
    private final IJobHandler jobHandler;

    /**
     * @param pJobHandler
     *            the job handler
     */
    public StoppingJobSubscriber(final IJobHandler pJobHandler) {
        jobHandler = pJobHandler;
    }

    @Override
    public void handle(final TenantWrapper<StoppingJobEvent> pStoppingJobEventWrapped) {
        final Long jobInfoId = pStoppingJobEventWrapped.getContent().getJobInfoId();
        final StatusInfo jobInfoAborted = jobHandler.abort(jobInfoId);
        if (jobInfoAborted.getJobStatus().equals(JobStatus.ABORTED)) {
            LOG.info(String.format("Job [%d] correctly stopped", jobInfoId));
        } else {
            LOG.warn(String.format("Job [%d] state [%s] was not stopped correctly", jobInfoId,
                                   jobInfoAborted.getJobStatus().toString()));
        }
    }

}
