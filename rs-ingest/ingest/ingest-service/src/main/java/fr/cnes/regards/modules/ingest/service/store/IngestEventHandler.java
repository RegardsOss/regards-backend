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
package fr.cnes.regards.modules.ingest.service.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * Handler to update AIPEntity state when a AIPEvent is received from archiva storage.
 * @author Sébastien Binda
 */
@Component
public class IngestEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAIPService aipService;

    /**
     * {@link ISubscriber} instance
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Subscribe to DataStorageEvent in order to update AIPs state for each successfully stored AIP.
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AIPEvent.class, new AIPEventHandler());
        subscriber.subscribeTo(JobEvent.class, new JobHandler());
    }

    /**
     * Job handler
     *
     * @author Marc Sordi
     */
    private class AIPEventHandler implements IHandler<AIPEvent> {

        @Override
        public void handle(TenantWrapper<AIPEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                aipService.handleAipEvent(wrapper.getContent());
            } catch (Exception e) {
                LOGGER.error("Error occurs during AIP event handling", e);
                // FIXME add notification
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Job handler
     *
     * @author Marc Sordi
     */
    private class JobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            LOGGER.debug("Job event received with state \"{}\", tenant \"{}\" and job info id \"{}\"",
                         wrapper.getContent().getJobEventType(), wrapper.getTenant(), wrapper.getContent().getJobId());
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                aipService.handleJobEvent(wrapper.getContent());
            } catch (Exception e) {
                LOGGER.error("Error occurs during job event handling", e);
                // FIXME add notification
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
