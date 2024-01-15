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


package fr.cnes.regards.modules.ingest.service.notification;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import fr.cnes.regards.modules.ingest.service.settings.IngestSettingsService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.ingest.service.TestData.*;

/**
 * Test for {@link AIPNotificationService}
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_notification_service_it",
                                   "regards.amqp.enabled=true",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class AIPNotificationServiceIT extends IngestMultitenantServiceIT {

    private static final String SESSION = "SESSION" + "_" + "0001";

    @Autowired
    private StorageClientMock storageClient;

    /**
     * Repositories
     */

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IngestSettingsService ingestSettingsService;

    /**
     * Services
     */

    @Autowired
    private RequestService requestService;

    @Autowired
    private IOAISDeletionService oaisDeletionService;

    @Autowired
    private IAIPService aipService;

    @Override
    public void doInit() throws EntityException {
        initNotificationSettings(true);
    }

    @Test
    @Purpose("Test aip requests are ready to be notified and are deleted after a notification success")
    public void testNotificationSuccess() {
        storageClient.setBehavior(true, true);

        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        // ---------------------------------- INGEST REQUESTS ----------------------------------
        int nbSIP = 3;
        initData(nbSIP);
        testRequestsSuccess(nbSIP, THREE_SECONDS, TimeUnit.MILLISECONDS); //simulate notification success

        // --------------------------------- UPDATE REQUESTS -----------------------------------
        // Update all aips
        aipService.registerUpdatesCreator(AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(SESSION),
                                                                       Lists.newArrayList("ADDED_TAG"),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList()));
        testRequestsSuccess(nbSIP, THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);

        // --------------------------------- DELETION REQUESTS ---------------------------------
        // Delete all aips
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE);
        dto.withSession(SESSION);
        oaisDeletionService.registerOAISDeletionCreator(dto);
        assertDeletedAIPs(nbSIP, FIVE_SECONDS, TimeUnit.MILLISECONDS);
        testRequestsSuccess(nbSIP, THREE_SECONDS, TimeUnit.MILLISECONDS);
    }

    @Test
    @Purpose("Test aip requests are not deleted and in error state after a notification error")
    public void testNotificationError() {
        // mock the publish method to not broke other tests in notifier manager
        Mockito.doNothing().when(publisher).publish(Mockito.any(NotificationRequestEvent.class));

        // ---------------------------------- INGEST REQUESTS ----------------------------------
        int nbSIP = 3;
        // Create ingest requests
        initData(nbSIP);
        // Simulate notification errors
        testRequestsError(nbSIP, THREE_SECONDS, TimeUnit.MILLISECONDS);
        // Retry requests
        requestService.scheduleRequestRetryJob(new SearchRequestParameters().withRequestIpTypesIncluded(Set.of(
            RequestTypeEnum.INGEST)));
        testRequestsSuccess(nbSIP, THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);

        // --------------------------------- UPDATE REQUESTS -----------------------------------
        // Create aip update requests
        aipService.registerUpdatesCreator(AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(SESSION),
                                                                       Lists.newArrayList("ADDED_TAG"),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList(),
                                                                       Lists.newArrayList()));
        // Simulate notification errors
        testRequestsError(nbSIP, nbSIP * THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);
        // Retry requests
        requestService.scheduleRequestRetryJob(new SearchRequestParameters().withRequestIpTypesIncluded(Set.of(
            RequestTypeEnum.UPDATE)));
        testRequestsSuccess(nbSIP, THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);

        // --------------------------------- DELETION REQUESTS ---------------------------------
        // Create aip deletion requests
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE);
        dto.withSession(SESSION);
        oaisDeletionService.registerOAISDeletionCreator(dto);
        assertDeletedAIPs(nbSIP, THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);
        // Simulate notification errors
        testRequestsError(nbSIP, THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);
        // Retry requests
        requestService.scheduleRequestRetryJob(new SearchRequestParameters().withRequestIpTypesIncluded(Set.of(
            RequestTypeEnum.OAIS_DELETION)));
        testRequestsSuccess(nbSIP, THREE_SECONDS * nbSIP, TimeUnit.MILLISECONDS);
    }

    //----------------------------------
    // UTILS
    //----------------------------------

    /**
     * Create AIPs
     */
    private void initData(int nbSIP) {
        storageClient.setBehavior(true, true);
        for (int i = 0; i < nbSIP; i++) {
            // create aips
            publishSIPEvent(create(UUID.randomUUID().toString(), getRandomTags()),
                            getRandomStorage().get(0),
                            SESSION,
                            getRandomSessionOwner(),
                            getRandomCategories());
        }
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000L, SIPState.STORED, getDefaultTenant());
    }

    /**
     * Verify and simulate notification success
     */
    private void testRequestsSuccess(int nbRequestsExpected, long timeout, TimeUnit unit) {
        Awaitility.await().atMost(timeout, unit).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            // test requests are not deleted and ready to be notified
            List<AbstractRequest> abstractRequests = abstractRequestRepository.findAll();
            if (nbRequestsExpected == abstractRequests.size()) {
                if (!checkNotificationStateAndStep(abstractRequests, "notify")) {
                    return false;
                }

                // simulate notifications are in success
                if (!abstractRequests.isEmpty()) {
                    notificationService.handleNotificationSuccess(Sets.newHashSet(abstractRequests));
                }
                long count = abstractRequestRepository.count();
                if (count != 0) {
                    LOGGER.warn("All requests should have been deleted {}", count);
                }
                return count == 0;
            } else {
                LOGGER.warn("The number of requests created is not expected {}/{}",
                            nbRequestsExpected,
                            abstractRequests.size());
            }
            return false;
        });
    }

    /**
     * Verify and simulate notification error
     */
    private void testRequestsError(int nbRequestsExpected, long timeout, TimeUnit unit) {
        Awaitility.await().atMost(timeout, unit).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            // test requests are not deleted and ready to be notified
            List<AbstractRequest> abstractRequests = abstractRequestRepository.findAll();
            if (abstractRequests.size() != nbRequestsExpected) {
                LOGGER.warn("The number of requests created is not expected {}/{}",
                            abstractRequests.size(),
                            nbRequestsExpected);
                return false;
            }
            if (!checkNotificationStateAndStep(abstractRequests, "notify")) {
                return false;
            }

            // simulate notifications are in error
            notificationService.handleNotificationError(Sets.newHashSet(abstractRequests));
            // all requests should be present with error state and step
            long count = abstractRequestRepository.count();
            if (nbRequestsExpected != count) {
                LOGGER.warn("All requests should have been kept {}/{}", count, nbRequestsExpected);
                return false;
            }

            return checkNotificationStateAndStep(abstractRequests, "notify_error");
        });
    }

    /**
     * Verify steps and state of requests
     */
    private boolean checkNotificationStateAndStep(List<AbstractRequest> abstractRequests, String step) {
        IngestRequestStep ingest_step = null;
        InternalRequestState state = null;
        DeletionRequestStep deletion_step = null;
        IngestErrorType errorType = null;
        // Init notification steps for requests
        if (step.equals("notify")) {
            ingest_step = IngestRequestStep.LOCAL_TO_BE_NOTIFIED;
            deletion_step = DeletionRequestStep.LOCAL_TO_BE_NOTIFIED;
            // update step
            state = InternalRequestState.RUNNING;
        } else if (step.equals("notify_error")) {
            ingest_step = IngestRequestStep.REMOTE_NOTIFICATION_ERROR;
            deletion_step = DeletionRequestStep.REMOTE_NOTIFICATION_ERROR;
            // update step
            state = InternalRequestState.ERROR;
            errorType = IngestErrorType.NOTIFICATION;
        }

        // Check states and steps of requests
        Iterator<AbstractRequest> it = abstractRequests.iterator();
        AbstractRequest abstractRequest;
        if (!it.hasNext()) {
            LOGGER.warn("Request list is empty");
            return false;
        }
        while (it.hasNext()) {
            abstractRequest = it.next();
            // check state
            if (state != null && !state.equals(abstractRequest.getState()) || (errorType != null
                                                                               && !abstractRequest.getErrorType()
                                                                                                  .equals(errorType))) {
                LOGGER.warn("The request state is not the expected one");
                return false;
            }
            // check steps of different requests
            if (ingest_step != null && abstractRequest instanceof IngestRequest ingestRequest) {
                // The request step is not the expected one
                return ingest_step.equals(ingestRequest.getStep());
            } else if (deletion_step != null && abstractRequest instanceof OAISDeletionRequest deletionRequest) {
                // The request step is not the expected one
                return deletion_step.equals(deletionRequest.getStep());
            }
        }
        return true;
    }

    /**
     * Verify AIPs are deleted after an OAIS Deletion Request
     */
    public void assertDeletedAIPs(long nbAipDeletedExpected, long timeout, TimeUnit unit) {
        Awaitility.await().atMost(timeout, unit).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            long count = aipRepository.findAll().stream().filter(a -> a.getState() == AIPState.DELETED).count();
            if (nbAipDeletedExpected != count) {
                LOGGER.warn("AIPs were supposed to be marked as deleted {}/{}", count, nbAipDeletedExpected);
            }
            return nbAipDeletedExpected == count;
        });

    }

    /**
     * Verify all aips are updated after an AIP Update Request
     */
    public void assertModifiedAIPS(String tagExpected) {
        List<AIPEntity> aips = aipRepository.findAll();
        boolean isTagMissing = false;
        Iterator<AIPEntity> it = aips.iterator();
        AIPEntity aip;
        while (it.hasNext() && !isTagMissing) {
            aip = it.next();
            if (!aip.getTags().contains(tagExpected)) {
                isTagMissing = true;
            }
        }
        Assert.assertFalse("AIPs were not updated", isTagMissing);
    }

    /**
     * Change state of notification settings
     */
    private void initNotificationSettings(boolean state) throws EntityException {
        ingestSettingsService.setActiveNotification(state);
    }

}
