/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.job;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Listen for Jobs events
 *
 * @author Marc SORDI
 */
@Component
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(JobEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(JobEvent.class, new JobHandler());
    }

    /**
     * Job handler
     *
     * @author Marc Sordi
     */
    private class JobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                JobEvent jobEvent = wrapper.getContent();
                switch (jobEvent.getJobEventType()) {
                    case ABORTED:
                    case FAILED:
                        ingestRequestService.handleJobCrash(jobEvent);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                String message = String.format(
                    "Ingest job with id \"%s\" and status \"%s\" causes exception during its processing",
                    wrapper.getContent().getJobId(),
                    wrapper.getContent().getJobEventType());
                LOGGER.error(message, e);
                notificationClient.notify(message,
                                          "Ingest job event failure",
                                          NotificationLevel.ERROR,
                                          DefaultRole.ADMIN);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
