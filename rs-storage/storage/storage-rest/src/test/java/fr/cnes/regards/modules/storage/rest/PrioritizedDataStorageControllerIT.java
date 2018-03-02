package fr.cnes.regards.modules.storage.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class PrioritizedDataStorageControllerIT extends AbstractRegardsTransactionalIT {

    private static final String DATA_STORAGE_CONF_LABEL = "PrioritizedDataStorageControllerIT";

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

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
                                                                      DATA_STORAGE_CONF_LABEL,
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
                                                                      DATA_STORAGE_CONF_LABEL,
                                                                      parameters,
                                                                      0);
        PrioritizedDataStorage created = prioritizedDataStorageService.create(dataStorageConf);
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
