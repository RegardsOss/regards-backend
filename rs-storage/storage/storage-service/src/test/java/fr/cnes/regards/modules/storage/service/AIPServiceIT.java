package fr.cnes.regards.modules.storage.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.*;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.local.LocalDataStorage;
import fr.cnes.regards.plugins.utils.PluginUtils;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { StoreJobIT.Config.class })
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AIPServiceIT extends AbstractRegardsServiceIT {

    private static final String ALLOCATION_CONF_LABEL = "AIPServiceIT_ALLOCATION";

    private static final String DATA_STORAGE_CONF_LABEL = "AIPServiceIT_DATA_STORAGE";

    private boolean failed;

    @Configuration
    static class Config {

        @Bean
        public IResourceService resourceService() {
            return Mockito.mock(IResourceService.class);
        }

    }

    @Autowired
    private Gson gson;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoRepository jobRepo;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private ISubscriber subscriber;

    @Value("${regards.storage.workspace}")
    private String workspace;

    private AIP aip;

    private Set<UUID> succeeded;

    private URL baseStorageLocation;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        failed = false;
        succeeded = Sets.newConcurrentHashSet();
        subscriber.subscribeTo(JobEvent.class, new JobEventHandler());
        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets create a plugin configuration for IAllocationStrategy
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultAllocationStrategyPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                                                         DefaultAllocationStrategyPlugin.class
                                                                                 .getPackage().getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, ALLOCATION_CONF_LABEL);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets create a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, IDataStorage.class.getPackage().getName(),
                                      IOnlineDataStorage.class.getPackage().getName());
        baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/AIPServiceIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation)).getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, DATA_STORAGE_CONF_LABEL, parameters,
                                                                      0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);
    }

    @Test
    public void createSuccessTest() throws ModuleException, InterruptedException {
        Set<UUID> jobIds = aipService.create(Sets.newHashSet(aip));
        while (!succeeded.containsAll(jobIds) && !failed) {
            //lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
        }
        Assert.assertFalse("The job failed while it should not have", failed);
        // lets wait two minutes so the metadata has been stored and check that everything is good
        Thread.sleep(2 * 60 * 1000);
        AIP result = aipDao.findOneByIpId(aip.getIpId());
        Assert.assertEquals(AIPState.STORED, result.getState());
        Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
        Assert.assertEquals(2, dataFiles.size());
    }

    @Test
    public void createFailOnDataTest() throws MalformedURLException, ModuleException, InterruptedException {
        //first lets change the data location to be sure it fails
        aip.getInformationObjects().get(0).getContentInformation().getDataObject().setUrl(new URL("file", "",
                                                                                                  System.getProperty(
                                                                                                          "user.dir")
                                                                                                          + "/src/test/resources/data_that_does_not_exists.txt"));
        Set<UUID> jobIds = aipService.create(Sets.newHashSet(aip));
        while (!succeeded.containsAll(jobIds) && !failed) {
            //lets wait for 1 sec before checking again if all our jobs has been done or not
            Thread.sleep(1000);
        }
        Assert.assertTrue("The job succeeded while it should not have", failed);
        // lets wait two minutes to be sure that metadata storage method was executed and check state of metadata
        Thread.sleep(2 * 60 * 1000);
        AIP result = aipDao.findOneByIpId(aip.getIpId());
        Assert.assertEquals(AIPState.STORAGE_ERROR, result.getState());
        Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.ERROR, aip);
        Assert.assertEquals(1, dataFiles.size());
    }

    @Test
    public void createSuccessAfterFailOnDataTest() throws InterruptedException, ModuleException, MalformedURLException {
        // lets not rewrite code
        createFailOnDataTest();
        //now lets correct what we did so we can store the metadata
        aip.getInformationObjects().get(0).getContentInformation().getDataObject()
                .setUrl(new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data.txt"));
        //reset the fail indicator
        failed = false;
        createSuccessTest();
    }

    @Test
    public void createFailOnMetadataTest() throws ModuleException, InterruptedException, IOException {
        // to make the process fail just on metadata storage, lets remove permissions from the workspace
        Path workspacePath = Paths.get(workspace, DEFAULT_TENANT);
        Set<PosixFilePermission> oldPermissions = Files.getPosixFilePermissions(workspacePath);
        Files.setPosixFilePermissions(workspacePath, Sets.newHashSet());
        try {
            Set<UUID> jobIds = aipService.create(Sets.newHashSet(aip));
            while (!succeeded.containsAll(jobIds) && !failed) {
                //lets wait for 1 sec before checking again if all our jobs has been done or not
                Thread.sleep(1000);
            }
            Assert.assertFalse("The job failed while it should not have", failed);
            // lets wait two minutes to be sure that metadata storage method was executed and check state of metadata
            Thread.sleep(2 * 60 * 1000);
            AIP result = aipDao.findOneByIpId(aip.getIpId());
            Assert.assertEquals(AIPState.STORAGE_ERROR, result.getState());
            Set<DataFile> dataFiles = dataFileDao.findAllByStateAndAip(DataFileState.STORED, aip);
            Assert.assertEquals(1, dataFiles.size());
        } finally {
            // to avoid issues with following tests, lets set back the permissions
            Files.setPosixFilePermissions(workspacePath, oldPermissions);
        }
    }

    private class JobEventHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            JobEvent event = wrapper.getContent();
            switch (event.getJobEventType()) {
                case ABORTED:
                case FAILED:
                    failed = true;
                    getLogger().error("Job " + event.getJobId() + " failed or was aborted");
                    break;
                case SUCCEEDED:
                    succeeded.add(event.getJobId());
                    break;
                default:
                    break;
            }
        }
    }

    private AIP getAIP() throws MalformedURLException {
        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        ContentInformation ci = new ContentInformation(new DataObject(DataType.RAWDATA, new URL("file", "", path)),
                                                       new RepresentationInformation(
                                                               new Syntax("description", "text/plain", "text")));
        AccessRightInformation ari = new AccessRightInformation("publisherDID", "publisherID", "public");
        FixityInformation fi = new FixityInformation("MD5", "de89a907d33a9716d11765582102b2e0");
        ProvenanceInformation pi = new ProvenanceInformation("CS", Lists.newArrayList(
                new Event("test event", OffsetDateTime.now(), EventType.SUBMISSION)));
        PreservationDescriptionInformation pdi = new PreservationDescriptionInformation(ari, fi, pi);
        InformationObject io = new InformationObject(ci, pdi);
        AIP aip = new AIP(EntityType.DATA);
        aip.setIpId(new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1)
                            .toString());
        aip.setInformationObjects(Lists.newArrayList(io));
        return aip;
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        jobRepo.deleteAll();
        pluginRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        Path defaultTenantWorkspace = Paths.get(workspace, DEFAULT_TENANT);
        // in other word, remove everything inside defaultTenantWorkspace but the directory defaultTenantWorkspace, service is not created on each test so workspace is not either
        Files.walk(defaultTenantWorkspace).sorted(Comparator.reverseOrder()).map(Path::toFile).filter(f -> !f.equals(defaultTenantWorkspace.toFile()))
                .forEach(File::delete);
        Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .forEach(File::delete);
    }

}
