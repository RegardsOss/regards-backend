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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.plugin.AIPPostProcessFailTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.AIPPostProcessTestPlugin;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.awaitility.Awaitility;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Sebastien Binda
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=post_process_job_it",
                                   "regards.amqp.enabled=true",
                                   "regards.ingest.aip.post-process.bulk.delay.init=100",
                                   "regards.ingest.aip.post-process.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class IngestPostProcessingJobIT extends IngestMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestPostProcessingJobIT.class);

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRepo;

    @Autowired
    private StorageClientMock storageClient;

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY", "CATEGORY00", "CATEGORY01");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY2");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("plop", "ping");

    private static final List<String> TAG_3 = Lists.newArrayList("toto");

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String STORAGE_3 = "Pentagon";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    private static final String SESSION_0 = OffsetDateTime.now().toString();

    private static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    private boolean isToNotify;

    @Override
    public void doInit() {
        this.isToNotify = initDefaultNotificationSettings();
    }

    public void initData(String chain) throws ModuleException {
        long nbSIP = 6;
        storageClient.setBehavior(true, true);
        publishSIPEvent(create("1", TAG_0),
                        Lists.newArrayList(STORAGE_1),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.of(chain));
        publishSIPEvent(create("2", TAG_0),
                        Lists.newArrayList(STORAGE_1),
                        SESSION_0,
                        SESSION_OWNER_1,
                        CATEGORIES_1,
                        Optional.of(chain));
        publishSIPEvent(create("3", TAG_1),
                        Lists.newArrayList(STORAGE_1),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.of(chain));
        publishSIPEvent(create("4", TAG_1),
                        Lists.newArrayList(STORAGE_1),
                        SESSION_1,
                        SESSION_OWNER_1,
                        CATEGORIES_1,
                        Optional.of(chain));
        publishSIPEvent(create("5", TAG_1),
                        Lists.newArrayList(STORAGE_2),
                        SESSION_1,
                        SESSION_OWNER_1,
                        CATEGORIES_0,
                        Optional.of(chain));
        publishSIPEvent(create("6", TAG_0),
                        Lists.newArrayList(STORAGE_2),
                        SESSION_1,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.of(chain));

        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, TEN_SECONDS * nbSIP, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitDuring(TWO_SECONDS * nbSIP);
        if (!isToNotify) {
            ingestServiceTest.waitAllRequestsFinished(TEN_SECONDS * nbSIP, getDefaultTenant());
        } else {
            mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
            ingestServiceTest.waitAllRequestsFinished(TWO_SECONDS * nbSIP, getDefaultTenant());
        }
    }

    @Test
    @Purpose("Check if AIPs where successfully postprocessed")
    public void checkPostProcess() throws ModuleException {
        // Creates a test chain with default post processing plugin
        createChainWithPostProcess(CHAIN_PP_LABEL, AIPPostProcessTestPlugin.class);
        initData(CHAIN_PP_LABEL);
        // Wait for postprocess requests ends
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return aipPostProcessRepo.count() == 0;
        });
        Assert.assertEquals(0, aipPostProcessRepo.count());

    }

    @Test
    @Purpose("Check if AIPs where postprocessed with errors")
    public void checkPostProcessWithErrors() throws ModuleException {
        // Creates a test chain with default post processing plugin
        createChainWithPostProcess(CHAIN_PP_WITH_ERRORS_LABEL, AIPPostProcessFailTestPlugin.class);
        initData(CHAIN_PP_WITH_ERRORS_LABEL);
        Page<AIPPostProcessRequest> pageRequests = aipPostProcessRepo.findAllByState(InternalRequestState.ERROR,
                                                                                     PageRequest.of(0, 100));
        Assert.assertEquals(3, pageRequests.getTotalElements());
        List<AIPPostProcessRequest> requests = pageRequests.getContent();
        Assert.assertTrue(requests.stream()
                                  .allMatch(req -> req.getState().equals(InternalRequestState.ERROR)
                                                   && req.getErrorType().equals(IngestErrorType.POSTPROCESSING)));
    }
}



