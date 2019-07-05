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
package fr.cnes.regards.modules.storage.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;
import fr.cnes.regards.modules.storage.service.job.AbstractStoreFilesJob;
import fr.cnes.regards.modules.storage.service.job.StorageJobProgressManager;
import fr.cnes.regards.modules.storage.service.job.StoreDataFilesJob;
import fr.cnes.regards.modules.storage.service.job.StoreMetadataFilesJob;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_test", "regards.amqp.enabled=true" },
        locations = { "classpath:storage.properties" })
@ActiveProfiles({ "testAmqp", "disableStorageTasks", "noschdule" })
@DirtiesContext(hierarchyMode = HierarchyMode.EXHAUSTIVE, classMode = ClassMode.BEFORE_CLASS)
public class StoreJobIT extends AbstractRegardsServiceTransactionalIT {

    private static final String LOCAL_STORAGE_LABEL = "StoreJobIT";

    @SuppressWarnings("unused")
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private LocalWorkingSubset workingSubset;

    private PluginConfiguration localStorageConf;

    private Set<JobParameter> parameters;

    private StorageDataFile df;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IPluginService pluginService;

    private URL baseStorageLocation;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private Gson gson;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Before
    public void init() throws IOException, URISyntaxException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        // first lets get some parameters for the job ...
        // ... dataStorage ...
        baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/StoreJobIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<PluginParameter> pluginParameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000L).getParameters();
        // new plugin conf for LocalDataStorage storage into target/LocalDataStorageIT
        PluginMetaData localStorageMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        localStorageConf = new PluginConfiguration(localStorageMeta, LOCAL_STORAGE_LABEL, pluginParameters);
        localStorageConf = pluginService.savePluginConfiguration(localStorageConf);
        // ... a working subset
        URL source = new URL("file", "", Paths.get("src", "test", "resources", "data.txt").toAbsolutePath().toString());
        AIP aip = getAipFromFile(false);
        aip.addEvent(EventType.SUBMISSION.name(), "submission into our beautiful system");
        AIPSession aipSession = new AIPSession();
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession.setId(aip.getSession());

        df = new StorageDataFile(Sets.newHashSet(source),
                                 "de89a907d33a9716d11765582102b2e0",
                                 "MD5",
                                 DataType.OTHER,
                                 0L,
                                 new MimeType("text", "plain"),
                                 new AIPEntity(aip, aipSession),
                                 "data.txt",
                                 null);
        workingSubset = new LocalWorkingSubset(Sets.newHashSet(df));
        // now that we have some parameters, lets storeAndCreate the job
        parameters = Sets.newHashSet();
        parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, localStorageConf.getId()));
        parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
    }

    @Test
    public void storeDataFilesJobTest() throws IOException, URISyntaxException, ModuleException {

        JobInfo toTest = new JobInfo(false, 0, parameters, getDefaultUserEmail(), StoreDataFilesJob.class.getName());
        StoreDataFilesJob job = (StoreDataFilesJob) runJob(toTest);
        // now that we synchronously ran the job, lets do some asserts
        StorageJobProgressManager progressManager = job.getProgressManager();
        Assert.assertFalse("there was a problem during the job", progressManager.isProcessError());
    }

    @Test
    public void storeQuicklookJobTest() throws IOException {
        URL source = new URL("file",
                             "",
                             Paths.get("src", "test", "resources", "quicklook.png").toAbsolutePath().toString());
        AIP aip = getAipFromFile(true);
        aip.addEvent(EventType.SUBMISSION.name(), "submission into our beautiful system");
        AIPSession aipSession = new AIPSession();
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession.setId(aip.getSession());

        StorageDataFile df = new StorageDataFile(Sets.newHashSet(source),
                                                 "540e72d5ac22f25c70d9c72b9b36fb96",
                                                 "MD5",
                                                 DataType.QUICKLOOK_SD,
                                                 0L,
                                                 new MimeType("image", "png"),
                                                 new AIPEntity(aip, aipSession),
                                                 "quicklook.png",
                                                 null);
        IWorkingSubset workingSubset = new LocalWorkingSubset(Sets.newHashSet(df));

        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters
                .add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, localStorageConf.getId()));
        jobParameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));

        JobInfo toTest = new JobInfo(false, 0, jobParameters, getDefaultUserEmail(), StoreDataFilesJob.class.getName());
        StoreDataFilesJob job = (StoreDataFilesJob) runJob(toTest);
        // now that we synchronously ran the job, lets do some asserts
        StorageJobProgressManager progressManager = job.getProgressManager();
        Assert.assertFalse("there was a problem during the job", progressManager.isProcessError());
        Assert.assertTrue(progressManager.getHandledDataFile().size() == 1);
        Assert.assertEquals("PNG should have a width of 1123 pixel",
                            Integer.valueOf(1123),
                            progressManager.getHandledDataFile().toArray(new StorageDataFile[0])[0].getWidth());
        Assert.assertEquals("PNG should have a height of 764 pixel",
                            Integer.valueOf(794),
                            progressManager.getHandledDataFile().toArray(new StorageDataFile[0])[0].getHeight());
    }

    @Test
    public void storeMetadataFilesJobTest() {
        JobInfo toTest = new JobInfo(false,
                                     0,
                                     parameters,
                                     getDefaultUserEmail(),
                                     StoreMetadataFilesJob.class.getName());
        StoreMetadataFilesJob job = (StoreMetadataFilesJob) runJob(toTest);
        // now that we synchronously ran the job, lets do some asserts
        StorageJobProgressManager progressManager = job.getProgressManager();
        Assert.assertFalse("there was a problem during the job", progressManager.isProcessError());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected IJob runJob(JobInfo jobInfo) {
        try {

            /**
             * JobInfo createJobInfo = jobInfoService.createAsQueued(jobInfo);
             * IJob job = createJobInfo.getJob();
             */
            IJob job = (IJob) Class.forName(jobInfo.getClassName()).newInstance();
            beanFactory.autowireBean(job);
            job.setParameters(jobInfo.getParametersAsMap());
            if (job.needWorkspace()) {
                job.setWorkspace(workspaceService::getPrivateDirectory);
            }
            jobInfo.setJob(job);
            jobInfo.getStatus().setStartDate(OffsetDateTime.now());
            job.run();
            return job;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            getLogger().error("Unable to instantiate job", e);
            Assert.fail("Unable to instantiate job");
        } catch (JobWorkspaceException e) {
            getLogger().error("Unable to set workspace", e);
            Assert.fail("Unable to set workspace");
        } catch (JobParameterMissingException e) {
            getLogger().error("Missing parameter", e);
            Assert.fail("Missing parameter");
        } catch (JobParameterInvalidException e) {
            getLogger().error("Invalid parameter", e);
            Assert.fail("Invalid parameter");
        }
        return null;
    }

    private AIP getAipFromFile(boolean quicklook) throws IOException {
        String fileName;
        if (quicklook) {
            fileName = "src/test/resources/aip_quicklook.json";
        } else {
            fileName = "src/test/resources/aip_sample.json";
        }
        try (JsonReader reader = new JsonReader(new FileReader(fileName))) {
            JsonElement el = Streams.parse(reader);
            String fileLine = el.toString();
            AIP aip = gson.fromJson(fileLine, AIP.class);

            return aip;
        }
    }

    @After
    public void after() throws URISyntaxException, IOException {
        tenantResolver.forceTenant(getDefaultTenant());
        Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .forEach(File::delete);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        pluginRepo.deleteAll();
        aipDao.deleteAll();
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
    }

}
