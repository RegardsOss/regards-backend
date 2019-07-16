/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.DataStorageEventHandler;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public abstract class AbstractAIPControllerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    static class Config {

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }
    }

    protected static final String ALLOCATION_CONF_LABEL = "AIPControllerIT_ALLOCATION";

    protected static final String DATA_STORAGE_CONF_LABEL = "AIPControllerIT_DATA_STORAGE";

    protected static final String CATALOG_SECURITY_DELEGATION_LABEL = "AIPControllerIT_SECU_DELEG";

    protected static final String SESSION = "Session123";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected Gson gson;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected ISubscriber subscriber;

    @Autowired
    protected IPluginConfigurationRepository pluginRepo;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IDataFileDao dataFileDao;

    @Autowired
    protected IJobInfoService jobInfoService;

    @Autowired
    protected IAIPDao aipDao;

    @Autowired
    protected IAIPUpdateRequestRepository aipUpdateRepo;

    @Autowired
    protected IAIPSessionRepository aipSessionRepo;

    @Autowired
    protected IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    protected IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    protected URL baseStorageLocation;

    protected AIP aip;

    @Autowired
    private IPluginService pluginService;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException, InterruptedException {
        cleanUp(false);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class);
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, ALLOCATION_CONF_LABEL);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets storeAndCreate a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta,
                                                                      DATA_STORAGE_CONF_LABEL,
                                                                      parameters,
                                                                      0);
        dataStorageConf.setIsActive(true);
        prioritizedDataStorageService.create(dataStorageConf);
        // forth, lets configure a plugin for security checks
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(FakeSecurityDelegation.class);
        PluginConfiguration catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta,
                                                                           CATALOG_SECURITY_DELEGATION_LABEL);
        pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException, InterruptedException {
        cleanUp(false);
    }

    public void cleanUp(boolean haveFailed) throws URISyntaxException, IOException, InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        logger.info("Waiting for current jobs finished ....");
        waitForJobsFinished(10, true);
        logger.info("All current jobs finished !");
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        try {
            jobInfoRepo.deleteAll();
            dataFileDao.deleteAll();
            aipUpdateRepo.deleteAll();
            aipDao.deleteAll();
            prioritizedDataStorageRepository.deleteAll();
            pluginRepo.deleteAll();
        } catch (DataIntegrityViolationException e) {
            logger.warn("Something went wrong while cleaning up database", e.getMessage());
            // Sometimes there is a problem to clean up entities stored in the database,
            // so if that's occurs, let's try another time
            if (!haveFailed) {
                // Try another time
                cleanUp(true);
                return;
            }
        }
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    protected AIP getAIP() throws MalformedURLException {
        return getNewAip(SESSION);
    }

    protected AIP getNewAip(String aipSession) throws MalformedURLException {
        return getNewAipWithTags(aipSession, "tag");
    }

    protected AIP getNewAip(AIPSession aipSession) throws MalformedURLException {
        return getNewAip(aipSession.getId());
    }

    protected AIP getNewAipWithTags(AIPSession aipSession, String... tags) throws MalformedURLException {
        return getNewAipWithTags(aipSession.getId(), tags);
    }

    protected AIP getNewAipWithTags(String aipSession, String... tags) throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP,
                                                            EntityType.DATA,
                                                            getDefaultTenant(),
                                                            UUID.randomUUID(),
                                                            1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP,
                                                            EntityType.DATA,
                                                            getDefaultTenant(),
                                                            sipId.getEntityId(),
                                                            1);
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), "providerId", EntityType.DATA, aipSession);

        Path path = Paths.get("src", "test", "resources", "data.txt");
        aipBuilder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, path, "MD5", "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder()
                .addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event", OffsetDateTime.now());
        aipBuilder.addTags(tags);
        return aipBuilder.build();
    }

    protected void waitForJobsFinished(int nbMaxSeconds, boolean forceStop) throws InterruptedException {
        List<JobInfo> jobs = Lists.newArrayList();
        Set<JobInfo> unfinishedJobs;
        int cpt = 0;
        do {
            jobs = jobInfoService.retrieveJobs();
            unfinishedJobs = jobs.stream()
                    .filter(f -> !f.getStatus().getStatus().equals(JobStatus.SUCCEEDED) && !f.getStatus().getStatus()
                            .equals(JobStatus.FAILED) && !f.getStatus().getStatus().equals(JobStatus.ABORTED))
                    .collect(Collectors.toSet());
            logger.info("[TEST CLEAN] Waiting for {} Unfinished jobs", unfinishedJobs.size());
            if (forceStop) {
                unfinishedJobs.forEach(j -> {
                    logger.info("[TEST CLEAN] Trying to stop running job {}-{} [{}]",
                                j.getClassName(),
                                j.getId(),
                                j.getStatus());
                    jobInfoService.stopJob(j.getId());
                });
            }
            if (unfinishedJobs.size() > 0) {
                Thread.sleep(1000);
            }
            cpt++;

        } while ((cpt < nbMaxSeconds) && (unfinishedJobs.size() > 0));

    }
}
