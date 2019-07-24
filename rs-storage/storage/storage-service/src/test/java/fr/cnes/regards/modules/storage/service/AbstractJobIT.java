/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_test", "regards.amqp.enabled=true" },
        locations = { "classpath:storage.properties" })
@ActiveProfiles({ "testAmqp", "disableStorageTasks", "noschdule" })
@EnableAsync
public abstract class AbstractJobIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJobIT.class);

    public static final String SESSION = "SESSION 42";

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    protected Set<JobParameter> parameters;

    @Autowired
    protected IPluginConfigurationRepository pluginRepo;

    @Autowired
    protected IAIPDao aipDao;

    @Autowired
    protected IDataFileDao dataFileDao;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IRuntimeTenantResolver tenantResolver;

    @Autowired
    protected IAIPService aipService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Autowired
    private IAIPSessionRepository sessionRepo;

    @Before
    public void init() throws IOException, URISyntaxException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());

        // Clear jobs
        jobInfoRepo.deleteAll();

        AIPSession aipSession = aipService.getSession(SESSION, true);
        for (int i = 0; i < 20; i++) {
            AIP aip = getNewAipWithTags(aipSession.getId(), "first tag", "second tag");
            aip.setState(AIPState.STORED);
            aipDao.save(aip, aipSession);
        }
    }

    protected JobInfo waitForJobFinished() throws InterruptedException {
        // Wait until the job finishes
        Iterable<JobInfo> jobs = jobInfoRepo.findAll();
        LOG.debug("Number of jobs in db : {}", jobInfoRepo.count());
        Assert.assertTrue("should have 1 job queued/running", jobs.iterator().hasNext());
        JobInfo jobInfo = jobs.iterator().next();
        LOG.debug("Waiting for job {} to end succeed ...", jobInfo.getClassName());
        // this loop acts like a timeout
        for (int i = 0; i < 40; i++) {
            // Pause for 1 seconds
            Thread.sleep(1000);
            Optional<JobInfo> jobInfoRefreshed = jobInfoRepo.findById(jobInfo.getId());
            if (JobStatus.SUCCEEDED.equals(jobInfoRefreshed.get().getStatus().getStatus())) {
                break;
            }
        }
        return jobInfo;
    }

    protected AIP getNewAipWithTags(String aipSession, String... tags) throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA, getDefaultTenant(),
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, getDefaultTenant(),
                sipId.getEntityId(), 1);
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), "providerId", EntityType.DATA, aipSession);
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());
        aipBuilder.addTags(tags);
        return aipBuilder.build();
    }

    @After
    public void after() throws URISyntaxException, IOException {
        tenantResolver.forceTenant(getDefaultTenant());
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
        aipDao.deleteAll();
        sessionRepo.deleteAll();
    }
}
