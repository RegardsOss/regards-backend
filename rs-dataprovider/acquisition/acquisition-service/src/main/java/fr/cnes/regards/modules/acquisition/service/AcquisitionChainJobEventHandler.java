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
package fr.cnes.regards.modules.acquisition.service;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.notification.client.INotificationClient;

/**
 *
 * Handle job event, mostly for job failure
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class AcquisitionChainJobEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionChainJobEventHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IProductService productService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAcquisitionProcessingService acquisitionProcessingService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
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
            String tenant = wrapper.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            JobEvent jobEvent = wrapper.getContent();
            LOGGER.debug("Job event received with state \"{}\", tenant \"{}\" and job info id \"{}\"",
                         jobEvent.getJobEventType(), tenant, jobEvent.getJobId());
            try {
                switch (jobEvent.getJobEventType()) {
                    case FAILED:
                        handleJobFailure(jobEvent);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("Error occurs during job event handling", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                notificationClient.notify(sw.toString(), "Error occurs during job event handling",
                                          NotificationLevel.ERROR, DefaultRole.ADMIN);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

        private void handleJobFailure(JobEvent jobEvent) {
            // Load job info
            JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            if (jobInfo != null) {
                // First lets check which job failed. Then lets responsible service handle errors.
                String jobClassName = jobInfo.getClassName();
                if (SIPGenerationJob.class.getName().equals(jobClassName)) {
                    productService.handleSIPGenerationError(jobInfo);
                } else if (ProductAcquisitionJob.class.getName().equals(jobClassName)) {
                    acquisitionProcessingService.handleProductAcquisitionError(jobInfo);
                }
            }
        }
    }
}
