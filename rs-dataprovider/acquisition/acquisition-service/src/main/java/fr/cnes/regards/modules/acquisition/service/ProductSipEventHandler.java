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
package fr.cnes.regards.modules.acquisition.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.service.job.PostAcquisitionJob;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * This class is triggered on {@link SIPEvent} receipts
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Component
public class ProductSipEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSipEventHandler.class);

    /**
     * AMQP Message subscriber
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Resolver to retrieve request tenant
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Resolver to retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    IProductService productService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("ProductSipEventHandler subscribs to new SIPEvent events");
        subscriber.subscribeTo(SIPEvent.class, new ProductSipReadyEventHandler());
    }

    private class ProductSipReadyEventHandler implements IHandler<SIPEvent> {

        @Override
        public void handle(TenantWrapper<SIPEvent> wrapper) {
            runtimeTenantResolver.forceTenant(wrapper.getTenant());
            SIPEvent event = wrapper.getContent();
            LOGGER.debug("[{}] received event {}", event.getIpId(), event.getState());

            // Update product state
            productService.updateProductSIPState(event.getIpId(), event.getState());

            // Do post processing if SIP properly stored
            if (SIPState.STORED.equals(event.getState())) {
                JobInfo acquisition = new JobInfo();
                acquisition.setParameters(new JobParameter(PostAcquisitionJob.EVENT_PARAMETER, event));
                acquisition.setClassName(PostAcquisitionJob.class.getName());
                acquisition.setOwner(authResolver.getUser());
                acquisition = jobInfoService.createAsQueued(acquisition);
            }
        }
    }

}
