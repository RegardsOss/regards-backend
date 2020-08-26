/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.aip.AIPUpdateService;
import fr.cnes.regards.modules.ingest.service.plugin.AIPPostProcessFailTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.AIPPostProcessTestPlugin;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;

/**
 * @author Sebastien Binda
 * @author Iliana Ghazali
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=post_process_test", "regards.amqp.enabled=true" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
public class IngestPostProcessingJobTest extends IngestMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPUpdatesCreatorJobIT.class);

    @Autowired
    private AIPUpdateService aipUpdateService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRepo;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IJobInfoService jobInfoService;

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

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        abstractRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
    }

    public long initData(String chain) throws ModuleException {
        long nbSIP = 6;
        publishSIPEvent(create("1", TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0, Optional.of(chain));
        publishSIPEvent(create("2", TAG_0), STORAGE_1, SESSION_0, SESSION_OWNER_1, CATEGORIES_1, Optional.of(chain));
        publishSIPEvent(create("3", TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0, Optional.of(chain));
        publishSIPEvent(create("4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1, Optional.of(chain));
        publishSIPEvent(create("5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_0, Optional.of(chain));
        publishSIPEvent(create("6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0, Optional.of(chain));
        return nbSIP;
    }

    @Test
    @Purpose("Check if AIPs where successfully postprocessed")
    public void checkPostProcess() throws ModuleException {
        // Creates a test chain with default post processing plugin
        createChainWithPostProcess(CHAIN_PP_LABEL, AIPPostProcessTestPlugin.class);
        storageClient.setBehavior(true, true);
        long nbSIP = initData(CHAIN_PP_LABEL);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
        ingestServiceTest.waitAllRequestsFinished(FIVE_SECONDS * 3);
    }

    @Test
    @Purpose("Check if AIPs where postprocessed with errors")
    public void checkPostProcessWithErrors() throws ModuleException {
        // Creates a test chain with default post processing plugin
        createChainWithPostProcess(CHAIN_PP_WITH_ERRORS_LABEL, AIPPostProcessFailTestPlugin.class);
        storageClient.setBehavior(true, true);
        long nbSIP = initData(CHAIN_PP_WITH_ERRORS_LABEL);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
        ingestServiceTest.waitAllRequestsFinished(FIVE_SECONDS * 3);

        Assert.assertEquals(3, aipPostProcessRepo.findAllByState(InternalRequestState.ERROR, PageRequest.of(0,100)).getTotalElements());
    }

}



