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

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * @author Léo Mieulet
 */
public class NewJobPuller implements INewJobPuller {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NewJobPuller.class);

    /**
     * Poller instance
     */
    private final Poller poller;

    /**
     * @param pPoller
     *            poller instance
     */
    public NewJobPuller(final Poller pPoller) {
        super();
        poller = pPoller;
    }

    /**
     * @param pProjectName
     *            the project name
     * @return a new jobInfo id
     */
    @Override
    public Long getJob(final String pProjectName) {
        Long jobInfoId = null;
        try {
            final TenantWrapper<NewJobEvent> tenantWrapper = poller.poll(NewJobEvent.class);
            final NewJobEvent newJobEvent = tenantWrapper.getContent();
            jobInfoId = newJobEvent.getJobInfoId();
        } catch (final RabbitMQVhostException e) {
            LOG.error(String.format("Failed to fetch a jobInfo for tenant [%s]", pProjectName), e);
        }
        return jobInfoId;
    }

}
