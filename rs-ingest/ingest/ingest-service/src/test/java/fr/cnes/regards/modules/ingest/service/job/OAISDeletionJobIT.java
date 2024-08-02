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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.modules.ingest.dao.AIPSpecificationsBuilder;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=deletion_job",
                                   "regards.amqp.enabled=true",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class OAISDeletionJobIT extends IngestMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestMultitenantServiceIT.class);

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IOAISDeletionService oaisDeletionService;

    @Autowired
    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    private IAIPRepository aipRepository;

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    private static final String SESSION_0 = OffsetDateTime.now().toString();

    private static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    private boolean isToNotify;

    @Override
    public void doInit() {
        this.isToNotify = initDefaultNotificationSettings();
    }

    public void waitUntilNbDeletionRequestInErrorReach(long timeout, long nbError, String tenant) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            return oaisDeletionRequestRepository.countByState(InternalRequestState.ERROR) == nbError;
        });
    }

    public void assertAipCount(long expectedCount, long timeout) {
        try {
            Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return aipRepository.count() == expectedCount;
            });
        } catch (ConditionTimeoutException e) {
            Assert.fail("AIPs count is not expected one after timeout");
        }
    }

    public void assertDeletedAIPs(long nbAipDeletedExpected, long timeout) {
        try {
            Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                List<AIPEntity> aips = aipRepository.findAll();
                long nb = 0;
                for (AIPEntity aip : aips) {
                    if (aip.getState() == AIPState.DELETED) {
                        nb = nb + 1;
                    }
                }
                return nbAipDeletedExpected == nb;
            });
        } catch (ConditionTimeoutException e) {
            Assert.fail("AIPs was supposed to be marked as deleted");
        }
    }

    public void initData() {

        long nbSIP = 6;
        publishSIPEvent(create("1", TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("2", TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("3", TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_0);
        publishSIPEvent(create("6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED, getDefaultTenant());
        long wait = FIVE_SECONDS * 3;
        if (!isToNotify) {
            ingestServiceTest.waitAllRequestsFinished(wait, getDefaultTenant());
        } else {
            mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
        }

        // Simulate associated udapte and post process requests
        aipRepository.findAll().forEach(aip -> {
            AIPUpdateParametersDto params = AIPUpdateParametersDto.build(new SearchAIPsParameters());
            params.setAddTags(List.of("newTag"));
            List<AIPUpdateRequest> updateRequests = AIPUpdateRequest.build(aip, params, true);
            updateRequests.forEach(ur -> ur.setState(InternalRequestState.ERROR));
            aipUpdateRequestRepository.saveAll(updateRequests);

            AIPPostProcessRequest postProcessRequest = AIPPostProcessRequest.build(aip, "toto");
            postProcessRequest.setState(InternalRequestState.ERROR);
            aipPostProcessRequestRepository.save(postProcessRequest);
        });
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_310"), @Requirement("REGARDS_DSL_STO_AIP_115") })
    @Purpose("Check deletion process for a list of SIPS. Check two deletion modes. Complete deletion or mark as "
             + "deleted")
    public void testDeletionJobSucceed() {
        ingestServiceTest.waitAllRequestsFinished(TEN_SECONDS * 3, getDefaultTenant());
        storageClient.setBehavior(true, true);
        initData();
        // delete 2 SIPs linked to SESSION_OWNER_0, SESSION_0
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE);
        dto.withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0);
        oaisDeletionService.registerOAISDeletionCreator(dto);
        assertDeletedAIPs(2, 20_000);
        // check if requests are deleted in case of notification
        if (isToNotify) {
            mockNotificationSuccess(RequestTypeConstant.OAIS_DELETION_VALUE);
        }

        // delete 1 SIP linked to SESSION_OWNER_0, SESSION_1
        dto = OAISDeletionPayloadDto.build(SessionDeletionMode.BY_STATE);
        dto.withSession(SESSION_1).withSessionOwner(SESSION_OWNER_0);
        oaisDeletionService.registerOAISDeletionCreator(dto);
        assertDeletedAIPs(3, 30_000);

        // check if requests are deleted in case of notification
        if (isToNotify) {
            mockNotificationSuccess(RequestTypeConstant.OAIS_DELETION_VALUE);
        }

        // delete 2 SIPs linked to SESSION_OWNER_1, SESSION_1
        dto = OAISDeletionPayloadDto.build(SessionDeletionMode.IRREVOCABLY);
        dto.withSession(SESSION_1).withSessionOwner(SESSION_OWNER_1);
        oaisDeletionService.registerOAISDeletionCreator(dto);
        long expectedRemainingAips = 4L;
        assertAipCount(expectedRemainingAips, 30_000); // AIPs are deleted and not just marked deleted

        // check if requests are deleted in case of notification
        if (isToNotify) {
            mockNotificationSuccess(RequestTypeConstant.OAIS_DELETION_VALUE);
        }

        // There should be only one AIP remaining in stored status
        long remainingStoredAips = aipRepository.findAll()
                                                .stream()
                                                .filter(aip -> aip.getState() == AIPState.STORED)
                                                .count();
        Assert.assertEquals(1L, remainingStoredAips);

        // Check update request deleted
        Assert.assertEquals(remainingStoredAips, aipUpdateRequestRepository.count());

        // check post process request deleted
        Assert.assertEquals(remainingStoredAips, aipPostProcessRequestRepository.count());

    }

    /**
     * Test that a deletion error from storage client does not affect results. AIPs & SIPs should be deleted
     * as well.
     */
    @Test
    public void testDeletionJobFailed() throws ModuleException, InterruptedException {
        storageClient.setBehavior(true, true);
        initData();
        storageClient.setBehavior(true, false);

        SearchAIPsParameters filters = new SearchAIPsParameters().withSessionOwner(SESSION_OWNER_0)
                                                                 .withSession(SESSION_0);
        Page<AIPEntity> aips = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters).build(),
                                                     PageRequest.of(0, 10));
        Assert.assertEquals(2, aips.getContent().size());
        // 2 SIPs linked to SESSION_OWNER_0, SESSION_0 will be marked as ERROR
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.IRREVOCABLY);
        dto.withSessionOwner(SESSION_OWNER_0).withSession(SESSION_0);
        oaisDeletionService.registerOAISDeletionCreator(dto);
        long wait = FIVE_SECONDS * 10;
        if (!isToNotify) {
            ingestServiceTest.waitAllRequestsFinished(wait, getDefaultTenant());
        } else {
            ingestServiceTest.waitDuring(wait);
            mockNotificationSuccess(RequestTypeConstant.OAIS_DELETION_VALUE);
        }
        aips = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters).build(),
                                     PageRequest.of(0, 10));
        Assert.assertEquals(0, aips.getContent().size());

    }
}
