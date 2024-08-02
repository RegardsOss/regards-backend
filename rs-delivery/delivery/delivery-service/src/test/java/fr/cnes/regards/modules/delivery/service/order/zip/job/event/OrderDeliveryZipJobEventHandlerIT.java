/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.zip.job.event;

import fr.cnes.regards.framework.integration.test.job.AbstractMultitenantServiceWithJobIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.modules.delivery.dao.IDeliveryAndJobRepository;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.config.OrderDeliveryTestConfiguration;
import fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Test for {@link OrderDeliveryZipJobEventHandler}.
 * <p>The purpose of this test is to check if final delivery zip job event is handled properly, ie, delivery
 * request should be in error if the job is in error and the link between delivery request and the job should be deleted
 * .</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link this#givenFailedJobEvent_whenSentAndHandleWithConcurrency_thenHandledOK()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link this#givenFailedJobEvent_whenSentAndHandleWithConcurrencyFailed_thenHandledKO()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "testAmqp", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=delivery_zip_job_event_handler_it",
                                   "regards.amqp.enabled=true",
                                   "regards.delivery.request.update.error.retries=2" })
@ContextConfiguration(classes = { OrderDeliveryTestConfiguration.class })
@SpringBootTest
public class OrderDeliveryZipJobEventHandlerIT extends AbstractMultitenantServiceWithJobIT {

    // SERVICES

    @Autowired
    private OrderDeliveryZipJobEventHandler orderDeliveryZipJobEventHandler; // class under test

    @SpyBean
    private DeliveryRequestService deliveryRequestService;

    // REPOSITORIES

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IDeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private IDeliveryAndJobRepository deliveryAndJobRepository;

    @Before
    public void init() throws Exception {
        clean();
    }

    @Test
    public void givenFailedJobEvent_whenSentAndHandleWithConcurrency_thenHandledOK() {
        // --- GIVEN ---
        DeliveryAndJob deliveryAndJob = initDeliveryRequestAndJob();
        DeliveryRequest deliveryRequest = deliveryAndJob.getDeliveryRequest();
        // simulate job event in error
        UUID jobId = deliveryAndJob.getJobInfo().getId();
        JobEvent jobEvent = new JobEvent(jobId, JobEventType.FAILED, OrderDeliveryZipJob.class.getName());
        // test concurrency by mocking find delivery request. It will return an old version of the request that has
        // been updated since.
        // The retry should succeed the second time because the real request will be retrieved.
        updateDeliveryRequest(deliveryRequest);
        Mockito.when(deliveryRequestService.findDeliveryRequest(deliveryRequest.getId()))
               .thenReturn(Optional.of(deliveryRequest))
               .thenCallRealMethod();

        // --- WHEN ---
        orderDeliveryZipJobEventHandler.handle(getDefaultTenant(), jobEvent);

        // --- THEN ---
        // delivery request was successfully handled
        Optional<DeliveryRequest> deliveryRequestUpdatedOpt = deliveryRequestRepository.findById(deliveryRequest.getId());
        Assertions.assertThat(deliveryRequestUpdatedOpt).isPresent();
        Assertions.assertThat(deliveryRequestUpdatedOpt.get().getStatus()).isEqualTo(DeliveryRequestStatus.ERROR);
        Assertions.assertThat(deliveryRequestUpdatedOpt.get().getErrorCause()).contains("An unexpected error occurred");
        // link between job and request was deleted
        Assertions.assertThat(deliveryAndJobRepository.findDeliveryRequestByJobId(jobId)).isNotPresent();
    }

    @Test
    public void givenFailedJobEvent_whenSentAndHandleWithConcurrencyFailed_thenHandledKO() {
        // --- GIVEN ---
        DeliveryAndJob deliveryAndJob = initDeliveryRequestAndJob();
        DeliveryRequest deliveryRequest = deliveryAndJob.getDeliveryRequest();
        // simulate job event in error
        UUID jobId = deliveryAndJob.getJobInfo().getId();
        JobEvent jobEvent = new JobEvent(jobId, JobEventType.FAILED, OrderDeliveryZipJob.class.getName());
        // test concurrency by mocking find delivery request. It will return an old version of the request that has
        // been updated since.
        // The retry should fail as the request could not be updated due to its old version.
        updateDeliveryRequest(deliveryRequest);
        Mockito.when(deliveryRequestService.findDeliveryRequest(deliveryRequest.getId()))
               .thenReturn(Optional.of(deliveryRequest));

        // --- WHEN ---
        try {
            orderDeliveryZipJobEventHandler.handle(getDefaultTenant(), jobEvent);
            Assertions.fail("OptimisticLockingFailureException should have been thrown");
        } catch (Exception e) {
            // --- THEN ---
            // request could not be updated despite the number of retries
            Assertions.assertThat(e).isInstanceOf(OptimisticLockingFailureException.class);
            // link between job and request is still present
            Assertions.assertThat(deliveryAndJobRepository.findDeliveryRequestByJobId(jobId)).isPresent();
        }
    }

    private void clean() {
        deliveryAndJobRepository.deleteAll();
        deliveryRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
    }

    private DeliveryAndJob initDeliveryRequestAndJob() {
        DeliveryRequest deliveryRequest = deliveryRequestRepository.save(DeliveryStepUtils.buildDeliveryRequest());
        JobInfo jobInfo = jobInfoRepository.save(new JobInfo(false,
                                                             0,
                                                             Set.of(),
                                                             null,
                                                             OrderDeliveryZipJob.class.getName()));
        return deliveryAndJobRepository.save(new DeliveryAndJob(deliveryRequest, jobInfo));
    }

    private void updateDeliveryRequest(DeliveryRequest deliveryRequest) {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        deliveryRequest.getDeliveryStatus().setStatus(DeliveryRequestStatus.DONE);
        deliveryRequestRepository.save(deliveryRequest);
    }
}
