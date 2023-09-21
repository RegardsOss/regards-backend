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
package fr.cnes.regards.modules.delivery.service.submission.update;

import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.StopJobEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.delivery.dao.IDeliveryAndJobRepository;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryStatus;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.mockito.Mockito.timeout;

/**
 * Test for {@link UpdateExpiredDeliveryRequestScheduler}.
 * <p>The purpose of this test is to check if {@link DeliveryRequest}s are properly handled when expired.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenExpiredRequests_whenExpireTaskRun_thenRequestsUpdate()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "test", "nojobs", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=delivery_delete_expired_it",
                                   "regards.delivery.request.expired.bulk.size=2",
                                   "regards.delivery.request.expired.jobs.bulk.size=2" })
@SpringBootTest
public class UpdateExpiredSchedulerIT extends AbstractMultitenantServiceIT {

    private UpdateExpiredDeliveryRequestScheduler updateScheduler; // class under test

    @Autowired
    private DeliveryRequestService deliveryRequestService;

    @Autowired
    private IDeliveryAndJobRepository deliveryAndJobRepository;

    @Autowired
    private IDeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private LockService lockService;

    @Autowired
    private UpdateExpiredService updateExpiredService;

    @Before
    public void init() {
        cleanRepositories();
        initService();

    }

    private void initService() {
        updateScheduler = new UpdateExpiredDeliveryRequestScheduler(tenantResolver,
                                                                    runtimeTenantResolver,
                                                                    lockService,
                                                                    updateExpiredService);
    }

    private void cleanRepositories() {
        deliveryAndJobRepository.deleteAll();
        deliveryRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
    }

    @Test
    public void givenExpiredRequests_whenExpireTaskRun_thenRequestsUpdate() {
        // GIVEN
        // delivery requests with some expired requests (this is not the case for all them)
        int nbDeliveryRequests = 5;
        List<DeliveryAndJob> deliveryRequestsAndJobs = initDeliveryRequestsAndJobs(nbDeliveryRequests);

        // WHEN
        updateScheduler.scheduleDeleteExpiredDeliveryRequests();

        // THEN
        // expect three delivery requests updated to error state because they have expired
        List<Long> expiredRequestsIds = deliveryRequestRepository.findAll()
                                                                 .stream()
                                                                 .filter(request -> request.getStatus()
                                                                                           .equals(DeliveryRequestStatus.ERROR)
                                                                                    && Objects.equals(request.getErrorType(),
                                                                                                      DeliveryErrorType.EXPIRED)
                                                                                    && Objects.requireNonNull(request.getErrorCause())
                                                                                              .contains("expiration"))
                                                                 .map(DeliveryRequest::getId)
                                                                 .toList();
        Assertions.assertThat(expiredRequestsIds).hasSize(3);
        // check that 2 stop job events were sent because they are linked to the expired requests and are still running
        // publisher is used 2 times because there are 2 transactions to send stop jobs events
        ArgumentCaptor<List<StopJobEvent>> jobEventsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, timeout(500).times(2)).publish(jobEventsCaptor.capture());
        List<StopJobEvent> stopJobsEvents = jobEventsCaptor.getAllValues().stream().flatMap(List::stream).toList();
        Assertions.assertThat(stopJobsEvents).hasSize(2);
        // check the correct jobs ids are ordered to stop
        Assertions.assertThat(stopJobsEvents.stream().map(StopJobEvent::getJobId).toList())
                  .containsExactlyInAnyOrderElementsOf(deliveryRequestsAndJobs.stream()
                                                                              .filter(deliveryAndJob ->
                                                                                          expiredRequestsIds.contains(
                                                                                              deliveryAndJob.getDeliveryRequest()
                                                                                                            .getId())
                                                                                          && deliveryAndJob.getJobInfo()
                                                                                                           .getStatus()
                                                                                                           .getStatus()
                                                                                                           .equals(
                                                                                                               JobStatus.RUNNING))
                                                                              .map(deliveryAndJobExpired -> deliveryAndJobExpired.getJobInfo()
                                                                                                                                 .getId())
                                                                              .toList());

    }

    private List<DeliveryAndJob> initDeliveryRequestsAndJobs(int nbDeliveryRequests) {
        return deliveryRequestService.saveAllRequests(buildDeliveryRequests(nbDeliveryRequests))
                                     .stream()
                                     .map(deliveryRequest -> {
                                         // build and save associated jobs (some are linked to expired requests and are still running)
                                         JobInfo linkedJobInfo = jobInfoRepository.save(buildLinkedJobInfo(
                                             deliveryRequest.getCorrelationId()));
                                         return deliveryAndJobRepository.save(new DeliveryAndJob(deliveryRequest,
                                                                                                 linkedJobInfo));
                                     })
                                     .toList();
    }

    public List<DeliveryRequest> buildDeliveryRequests(int nbRequest) {
        OffsetDateTime limitExpirationDate = OffsetDateTime.now();
        List<DeliveryRequest> deliveryRequests = new ArrayList<>(nbRequest);
        for (int i = 0; i < nbRequest; i++) {
            DeliveryRequest deliveryRequest = new DeliveryRequest("corrId-" + i,
                                                                  "regards-delivery@test.fr",
                                                                  new DeliveryStatus(OffsetDateTime.now(),
                                                                                     OffsetDateTime.now(),
                                                                                     1,
                                                                                     DeliveryRequestStatus.GRANTED,
                                                                                     null,
                                                                                     null),
                                                                  95L,
                                                                  1,
                                                                  "rs-delivery",
                                                                  1);

            // only the two last requests will not be expired when the task will be launched
            deliveryRequest.getDeliveryStatus().setExpiryDate(limitExpirationDate.minusHours(nbRequest - i - 3));
            deliveryRequests.add(deliveryRequest);
        }
        return deliveryRequests;
    }

    public JobInfo buildLinkedJobInfo(String correlationId) {
        JobInfo jobInfo = new JobInfo(false, 0, Set.of(), null, OrderDeliveryZipJob.class.getName());
        // only the first delivery request job is considered as done
        if (correlationId.equals("corrId-0")) {
            jobInfo.getStatus().setStatus(JobStatus.SUCCEEDED);
        } else {
            jobInfo.getStatus().setStatus(JobStatus.RUNNING);
        }
        return jobInfo;
    }

}
