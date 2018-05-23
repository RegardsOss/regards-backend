/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service;

import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;

public class StoreJobEventHandler implements IHandler<JobEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(StoreJobEventHandler.class);

    private Set<UUID> jobSucceeds = Sets.newHashSet();

    private boolean failed = false;

    @Override
    public synchronized void handle(TenantWrapper<JobEvent> wrapper) {
        JobEvent event = wrapper.getContent();
        switch (event.getJobEventType()) {
            case ABORTED:
            case FAILED:
                LOG.info("JobEvent Failure received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                failed = true;
                break;
            case SUCCEEDED:
                LOG.info("JobEvent Success received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                jobSucceeds.add(event.getJobId());
                break;
            case RUNNING:
                LOG.info("JobEvent Running received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                break;
            default:
                break;
        }
        LOG.info("JobEvent nb of succeed jobs = {}. class id={}", jobSucceeds.size(), this.hashCode());
    }

    public synchronized Set<UUID> getJobSucceeds() {
        LOG.info("Get succeed jobs = {}", jobSucceeds.size());
        return jobSucceeds;
    }

    public synchronized void setJobSucceeds(Set<UUID> pJobSucceeds) {
        LOG.info("Set succeed jobs = {}", jobSucceeds.size());
        jobSucceeds = pJobSucceeds;
    }

    public synchronized boolean isFailed() {
        return failed;
    }

    public synchronized void setFailed(boolean pFailed) {
        failed = pFailed;
    }

    public synchronized void reset() {
        failed = false;
        jobSucceeds.clear();
    }

}
