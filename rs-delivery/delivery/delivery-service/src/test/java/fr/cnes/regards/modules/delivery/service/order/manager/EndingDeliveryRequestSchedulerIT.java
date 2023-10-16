/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.manager;

import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dao.IDeliveryAndJobRepository;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryStatus;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.order.clean.job.CleanOrderJob;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob;
import fr.cnes.regards.modules.delivery.service.schedulers.EndingDeliveryRequestScheduler;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link EndingDeliveryRequestScheduler}.
 * <p>The purpose of this test is to verify that {@link DeliveryRequest}s in final states (DONE or ERROR) are
 * managed properly ({@link EndingDeliveryTask} is executed as expected)</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenFinishedErrorRequests_whenEndingTaskRun_thenErrorTasksExecuted()}</li>
 *      <li>{@link #givenFinishedDoneRequests_whenEndingTaskRun_thenDoneTasksExecuted()}</li>
 *    </ul>
 *  </li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "test", "nojobs", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=delivery_ending_it",
                                   "regards.delivery.request.finished.bulk.size=2" })
@SpringBootTest
public class EndingDeliveryRequestSchedulerIT extends AbstractMultitenantServiceIT {

    private EndingDeliveryRequestScheduler endingDeliveryRequestScheduler; // class under test

    @Autowired
    private EndingDeliveryService endingDeliveryService;

    @Autowired
    private IDeliveryAndJobRepository deliveryAndJobRepository;

    @Autowired
    private IDeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private LockService lockService;

    @Value("${regards.delivery.request.finished.bulk.size}")
    private int finishedRequestsPageSize;

    @Before
    public void init() {
        cleanRepositories();
        initService();
    }

    @Test
    public void givenFinishedErrorRequests_whenEndingTaskRun_thenErrorTasksExecuted() {
        // GIVEN
        // init delivery requests in error state (handled in a single page)
        int nbRequests = 2;
        List<DeliveryRequest> errorRequests = deliveryRequestRepository.saveAll(buildFinishedDeliveryRequests(nbRequests,
                                                                                                              DeliveryRequestStatus.ERROR));

        // WHEN
        endingDeliveryRequestScheduler.scheduleDeliveryRequestsEndingTask();

        // THEN
        // check ending tasks for requests in error are executed
        // clean job scheduled
        List<JobInfo> jobsInfo = jobInfoService.retrieveJobs();
        Assertions.assertThat(jobsInfo).hasSize(1);
        JobInfo queudJobInfo = jobsInfo.get(0);
        Assertions.assertThat(queudJobInfo.getClassName()).isEqualTo(CleanOrderJob.class.getName());
        Assertions.assertThat(queudJobInfo.getStatus().getStatus()).isEqualTo(JobStatus.QUEUED);

        Assertions.assertThat(jobInfoService.retrieveJob(queudJobInfo.getId()).getParameters())
                  .contains(new JobParameter(CleanOrderJob.CORRELATION_IDS,
                                             errorRequests.stream().map(DeliveryRequest::getCorrelationId).toList()));

        // delivery responses published
        List<DeliveryResponseDtoEvent> responseEvents = errorRequests.stream()
                                                                     .map(errorRequest -> new DeliveryResponseDtoEvent(
                                                                         errorRequest.getCorrelationId(),
                                                                         errorRequest.getStatus(),
                                                                         errorRequest.getErrorType(),
                                                                         errorRequest.getErrorCause(),
                                                                         null,
                                                                         null,
                                                                         errorRequest.getOriginRequestAppId(),
                                                                         errorRequest.getOriginRequestPriority()))
                                                                     .toList();
        Mockito.verify(publisher).publish(responseEvents);

        // check request deleted
        Assertions.assertThat(deliveryRequestRepository.findAll()).isEmpty();
    }

    @Test
    public void givenFinishedDoneRequests_whenEndingTaskRun_thenDoneTasksExecuted() {
        // GIVEN
        // init delivery requests in error state
        int nbRequests = 3;
        List<DeliveryRequest> doneRequests = deliveryRequestRepository.saveAll(buildFinishedDeliveryRequests(nbRequests,
                                                                                                             DeliveryRequestStatus.DONE));

        // WHEN
        endingDeliveryRequestScheduler.scheduleDeliveryRequestsEndingTask();

        // THEN
        // check ending tasks for done requests are executed
        // zip job scheduled
        List<JobInfo> jobsInfo = jobInfoService.retrieveJobs();
        Assertions.assertThat(jobsInfo).hasSize(nbRequests);
        jobsInfo.forEach(queudJobInfo -> {
            Assertions.assertThat(queudJobInfo.getClassName()).isEqualTo(OrderDeliveryZipJob.class.getName());
            Assertions.assertThat(queudJobInfo.getStatus().getStatus()).isEqualTo(JobStatus.QUEUED);

        });
        // check link between request and job is created
        List<DeliveryAndJob> deliveryAndJobLinks = deliveryAndJobRepository.findAll();
        Assertions.assertThat(deliveryAndJobLinks).hasSize(nbRequests);
        Assertions.assertThat(deliveryAndJobLinks.stream().map(DeliveryAndJob::getDeliveryRequest).toList())
                  .isEqualTo(doneRequests);
        Assertions.assertThat(deliveryAndJobLinks.stream().map(DeliveryAndJob::getJobInfo).toList())
                  .isEqualTo(jobsInfo);
    }

    private List<DeliveryRequest> buildFinishedDeliveryRequests(int nbRequests, DeliveryRequestStatus status) {
        List<DeliveryRequest> deliveryRequests = new ArrayList<>(nbRequests);
        for (int i = 0; i < nbRequests; i++) {
            if (status == DeliveryRequestStatus.DONE) {
                deliveryRequests.add(new DeliveryRequest("corrId-done-" + i,
                                                         "regards-delivery@test.fr",
                                                         new DeliveryStatus(OffsetDateTime.now(),
                                                                            OffsetDateTime.now(),
                                                                            1,
                                                                            DeliveryRequestStatus.DONE,
                                                                            null,
                                                                            null),
                                                         (long) i,
                                                         1,
                                                         "rs-delivery",
                                                         1));
            } else if (status == DeliveryRequestStatus.ERROR) {
                deliveryRequests.add(new DeliveryRequest("corrId-error-" + i,
                                                         "regards-delivery@test.fr",
                                                         new DeliveryStatus(OffsetDateTime.now(),
                                                                            OffsetDateTime.now(),
                                                                            1,
                                                                            DeliveryRequestStatus.ERROR,
                                                                            "the order failed",
                                                                            DeliveryErrorType.INTERNAL_ERROR),
                                                         (long) i,
                                                         1,
                                                         "rs-delivery",
                                                         1));
            }
        }
        return deliveryRequests;
    }

    private void initService() {
        endingDeliveryRequestScheduler = new EndingDeliveryRequestScheduler(tenantResolver,
                                                                            runtimeTenantResolver,
                                                                            lockService,
                                                                            endingDeliveryService,
                                                                            finishedRequestsPageSize);
    }

    private void cleanRepositories() {
        deliveryAndJobRepository.deleteAll();
        jobInfoRepository.deleteAll();
        deliveryRequestRepository.deleteAll();
    }

}
