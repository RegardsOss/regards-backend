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

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;

/**
 * Test {@link ProductService}
 * @author Marc Sordi
 *
 */
@Ignore("Testing handler")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product" })
@MultitenantTransactional
public class ProductServiceTest extends AbstractMultitenantServiceTest {

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IProductService productService;

    @Test
    public void testEventHandler() {
        JobInfo info = new JobInfo(false, 0, null, "test", SIPGenerationJob.class.getName());
        jobInfoService.createAsPending(info);

        // Simulate
        IJob<Void> job = new SIPGenerationJob();
        info.setJob(job);
        info.updateStatus(JobStatus.FAILED);
        jobInfoService.save(info);

        JobEvent event = new JobEvent(info.getId(), JobEventType.FAILED);
//        productService.handleAcquisitionChainJobEvent(event);
        productService.handleSIPGenerationError(info);
    }
}
