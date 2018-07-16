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
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
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
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IAIPSessionRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
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

    /**
     * Default tenant configured in application properties
     */
    protected static final String DEFAULT_TENANT = "PROJECT";

    private static final String ALLOCATION_CONF_LABEL = "AIPControllerIT_ALLOCATION";

    private static final String DATA_STORAGE_CONF_LABEL = "AIPControllerIT_DATA_STORAGE";

    private static final String CATALOG_SECURITY_DELEGATION_LABEL = "AIPControllerIT_SECU_DELEG";

    private static final String SESSION = "Session123";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    protected Gson gson;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IDataFileDao dataFileDao;

    @Autowired
    protected IAIPDao aipDao;

    @Autowired
    protected IAIPSessionRepository aipSessionRepo;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    private URL baseStorageLocation;

    protected AIP aip;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException, InterruptedException {
        cleanUp();
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // first of all, lets get an AIP with accessible dataObjects and real checksums
        aip = getAIP();
        // second, lets storeAndCreate a plugin configuration for IAllocationStrategy
        PluginMetaData allocationMeta = PluginUtils
                .createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                      DefaultAllocationStrategyPlugin.class.getPackage().getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta, ALLOCATION_CONF_LABEL);
        allocationConfiguration.setIsActive(true);
        pluginService.savePluginConfiguration(allocationConfiguration);
        // third, lets storeAndCreate a plugin configuration of IDataStorage with the highest priority
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, DATA_STORAGE_CONF_LABEL, parameters,
                0);
        dataStorageConf.setIsActive(true);
        prioritizedDataStorageService.create(dataStorageConf);
        // forth, lets configure a plugin for security checks
        pluginService.addPluginPackage(FakeSecurityDelegation.class.getPackage().getName());
        PluginMetaData catalogSecuDelegMeta = PluginUtils
                .createPluginMetaData(FakeSecurityDelegation.class, FakeSecurityDelegation.class.getPackage().getName(),
                                      ISecurityDelegation.class.getPackage().getName());
        PluginConfiguration catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta,
                CATALOG_SECURITY_DELEGATION_LABEL);
        pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException, InterruptedException {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
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
        AIPBuilder aipBuilder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1),
                null, EntityType.DATA, aipSession);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());
        aipBuilder.addTags(tags);
        return aipBuilder.build();
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
