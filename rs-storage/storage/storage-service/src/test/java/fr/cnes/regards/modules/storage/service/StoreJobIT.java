/*
 * LICENSE_PLACEHOLDER
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
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.annotation.DirtiesContext;
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
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
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
@TestPropertySource(locations = "classpath:test.properties")
@DirtiesContext
public class StoreJobIT extends AbstractRegardsServiceTransactionalIT {

    private static final String LOCAL_STORAGE_LABEL = "StoreJobIT";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private LocalWorkingSubset workingSubset;

    private PluginConfiguration localStorageConf;

    private Set<JobParameter> parameters;

    private DataFile df;

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
    private Gson gson;

    @Before
    public void init() throws IOException, URISyntaxException, ModuleException {
        tenantResolver.forceTenant(DEFAULT_TENANT);
        // first lets get some parameters for the job ...
        // ... dataStorage ...
        baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/StoreJobIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> pluginParameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .addParameter(LocalDataStorage.LOCAL_STORAGE_OCCUPIED_SPACE_THRESHOLD, "90")
                .getParameters();
        // new plugin conf for LocalDataStorage storage into target/LocalDataStorageIT
        PluginMetaData localStorageMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, LocalDataStorage.class.getPackage().getName(),
                                      IDataStorage.class.getPackage().getName());
        localStorageConf = new PluginConfiguration(localStorageMeta, LOCAL_STORAGE_LABEL, pluginParameters);
        localStorageConf = pluginService.savePluginConfiguration(localStorageConf);
        // ... a working subset
        URL source = new URL("file", "", "src/test/resources/data.txt");
        AIP aip = getAipFromFile();
        aip.addEvent(EventType.SUBMISSION.name(), "submission into our beautiful system");
        df = new DataFile(source, "de89a907d33a9716d11765582102b2e0", "MD5", DataType.OTHER, 0L,
                new MimeType("text", "plain"), aip, "data.txt");
        workingSubset = new LocalWorkingSubset(Sets.newHashSet(df));
        // now that we have some parameters, lets storeAndCreate the job
        parameters = Sets.newHashSet();
        parameters.add(new JobParameter(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME, localStorageConf.getId()));
        parameters.add(new JobParameter(AbstractStoreFilesJob.WORKING_SUB_SET_PARAMETER_NAME, workingSubset));
    }

    @Test
    public void storeDataFilesJobTest() throws IOException, URISyntaxException, ModuleException {

        JobInfo toTest = new JobInfo(0, parameters, DEFAULT_USER_EMAIL, StoreDataFilesJob.class.getName());
        StoreDataFilesJob job = (StoreDataFilesJob) runJob(toTest);
        // now that we synchronously ran the job, lets do some asserts
        StorageJobProgressManager progressManager = job.getProgressManager();
        Assert.assertFalse("there was a problem during the job", progressManager.isProcessError());
    }

    @Test
    public void storeMetadataFilesJobTest() {
        JobInfo toTest = new JobInfo(0, parameters, DEFAULT_USER_EMAIL, StoreMetadataFilesJob.class.getName());
        StoreMetadataFilesJob job = (StoreMetadataFilesJob) runJob(toTest);
        // now that we synchronously ran the job, lets do some asserts
        StorageJobProgressManager progressManager = job.getProgressManager();
        Assert.assertFalse("there was a problem during the job", progressManager.isProcessError());
    }

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
                job.setWorkspace(Files.createTempDirectory(jobInfo.getId().toString()));
            }
            jobInfo.setJob(job);
            jobInfo.getStatus().setStartDate(OffsetDateTime.now());
            job.run();
            return job;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
            getLogger().error("Unable to instantiate job", e);
            Assert.fail("Unable to instantiate job");
        } catch (JobParameterMissingException e) {
            getLogger().error("Missing parameter", e);
            Assert.fail("Missing parameter");
        } catch (JobParameterInvalidException e) {
            getLogger().error("Invalid parameter", e);
            Assert.fail("Invalid parameter");
        }
        return null;
    }

    private AIP getAipFromFile() throws IOException {

        try (JsonReader reader = new JsonReader(new FileReader("src/test/resources/aip_sample.json"))) {
            JsonElement el = Streams.parse(reader);
            String fileLine = el.toString();
            AIP aip = gson.fromJson(fileLine, AIP.class);

            return aip;
        }
    }

    @After
    public void after() throws URISyntaxException, IOException {
        Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .forEach(File::delete);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        pluginRepo.deleteAll();
        aipDao.deleteAll();
    }

}
