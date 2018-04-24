package fr.cnes.regards.modules.storage.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.DataStorageEventHandler;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
public class PrioritizedDataStorageControllerIT extends AbstractRegardsTransactionalIT {

    private static final String DATA_STORAGE_CONF_LABEL_1 = "PrioritizedDataStorageControllerIT_1";

    private static final String DATA_STORAGE_CONF_LABEL_2 = "PrioritizedDataStorageControllerIT_2";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Test
    public void testCreate() throws IOException, URISyntaxException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        URL baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta,
                                                                      DATA_STORAGE_CONF_LABEL_1,
                                                                      parameters,
                                                                      0);
        PrioritizedDataStorage toCreate = new PrioritizedDataStorage(dataStorageConf, null, DataStorageType.ONLINE);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content.id", Matchers.notNullValue(Long.class)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.dataStorageType",
                                                                               Matchers.is(DataStorageType.ONLINE
                                                                                                   .name())));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.priority", Matchers.is(0)));
        performDefaultPost(PrioritizedDataStorageController.BASE_PATH,
                           toCreate,
                           requestBuilderCustomizer,
                           "Could not create a PrioritizedDataStorage");
    }

    @Test
    public void testRetrieve() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.ID_PATH,
                          requestBuilderCustomizer,
                          "could not retrieve the prioritized data storage",
                          created.getId());
    }

    @Test
    public void testRetrieveByType() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeRequestParam().param("type", DataStorageType.ONLINE.name());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(PrioritizedDataStorageController.BASE_PATH,
                          requestBuilderCustomizer,
                          "could not retrieve the prioritized data storage");
    }

    @Test
    public void testUpdate() throws ModuleException, URISyntaxException, IOException {
        PrioritizedDataStorage created = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        created.getDataStorageConfiguration().setIsActive(false);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(
                "$.content.dataStorageConfiguration.active",
                Matchers.is(Boolean.FALSE)));
        performDefaultPut(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.ID_PATH,
                          created,
                          requestBuilderCustomizer,
                          "could not update the prioritized data storage",
                          created.getId());
    }

    @Test
    public void testIncreasePriority() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created1 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        PrioritizedDataStorage created2 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_2);
        Assert.assertEquals("created1 priority should be 0", 0L, created1.getPriority().longValue());
        Assert.assertEquals("created2 priority should be 1", 1L, created2.getPriority().longValue());
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.UP_PATH,
                          null,
                          requestBuilderCustomizer,
                          "could not increase the priority of created2",
                          created2.getId());
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        created1 = prioritizedDataStorageRepository.findOne(created1.getId());
        created2 = prioritizedDataStorageRepository.findOne(created2.getId());
        Assert.assertEquals("created2 should now has a priority of 0", 0L, created2.getPriority().longValue());
        Assert.assertEquals("created1 should now has a priority of 1", 1L, created1.getPriority().longValue());
    }

    @Test
    public void testDecreasePriority() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created1 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        PrioritizedDataStorage created2 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_2);
        Assert.assertEquals("created1 priority should be 0", 0L, created1.getPriority().longValue());
        Assert.assertEquals("created2 priority should be 1", 1L, created2.getPriority().longValue());
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPut(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.DOWN_PATH,
                          null,
                          requestBuilderCustomizer,
                          "could not decrease the priority of created1",
                          created1.getId());
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        created1 = prioritizedDataStorageRepository.findOne(created1.getId());
        created2 = prioritizedDataStorageRepository.findOne(created2.getId());
        Assert.assertEquals("created2 should now has a priority of 0", 0L, created2.getPriority().longValue());
        Assert.assertEquals("created1 should now has a priority of 1", 1L, created1.getPriority().longValue());
    }

    @After
    public void cleanUp() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
    }

    private PrioritizedDataStorage createPrioritizedDataStorage(String label)
            throws IOException, URISyntaxException, ModuleException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        URL baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        return prioritizedDataStorageService.create(dataStorageConf);
    }

    @Configuration
    static class Config {

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }
    }

}
