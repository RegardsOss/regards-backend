package fr.cnes.regards.modules.storage.service;

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
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.builder.InformationObjectBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.ICachedFileRepository;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.EventType;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.SimpleNearLineStoragePlugin;
import fr.cnes.regards.modules.storage.plugin.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.staf.STAFDataStorage;

@ContextConfiguration(classes = { StoreJobIT.Config.class })
@TestPropertySource(locations = "classpath:test.properties")
@ActiveProfiles("testAmqp")
public class AIPServiceRestoreIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(AIPServiceRestoreIT.class);

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private ICachedFileRepository cachedFileRepository;

    private PluginConfiguration dataStorageConf;

    private PluginConfiguration stafConf;

    private URL baseStorageLocation;

    private URL stafLocation;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        this.cleanUp();

        baseStorageLocation = new URL("file", "", Paths.get("target/AIPServiceIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));

        stafLocation = new URL("file", "", Paths.get("target/STAF").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(stafLocation.toURI()));

        // second, lets create a plugin configuration for IAllocationStrategy
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(INearlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(SimpleNearLineStoragePlugin.class.getPackage().getName());

        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation))
                .getParameters();
        dataStorageConf = new PluginConfiguration(dataStoMeta, "dsConfLabel", parameters, 0);
        dataStorageConf.setIsActive(true);
        pluginService.savePluginConfiguration(dataStorageConf);

        Gson ogson = new Gson();
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName("PLOP");
        archive.setPassword("pwd");
        PluginMetaData stafMeta = PluginUtils.createPluginMetaData(SimpleNearLineStoragePlugin.class,
                                                                   IDataStorage.class.getPackage().getName(),
                                                                   INearlineDataStorage.class.getPackage().getName());
        parameters = PluginParametersFactory.build()
                .addParameter(STAFDataStorage.STAF_ARCHIVE_PARAMETER_NAME, ogson.toJson(archive))
                .addParameter(STAFDataStorage.STAF_WORKSPACE_PATH, stafLocation.toString()).getParameters();
        stafConf = new PluginConfiguration(stafMeta, "stafConfLabel", parameters, 0);
        stafConf.setIsActive(true);

        pluginService.savePluginConfiguration(stafConf);
    }

    @Test
    public void loadUnavailableFilesTest() {
        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(response.getErrors().size() == 3);
    }

    private void fillOnlineDataFileDb() throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<DataFile> datafiles = Sets.newHashSet();
        URL url = new URL(Paths.get(baseStorageLocation.toString(), "file1.test").toString());
        DataFile df = new DataFile(url, "1", "MD5", DataType.RAWDATA, 50L, MimeType.valueOf("application/text"), aip,
                "file1.test");
        df.setDataStorageUsed(dataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file2.test").toString());
        df = new DataFile(url, "2", "MD5", DataType.RAWDATA, 50L, MimeType.valueOf("application/text"), aip,
                "file2.test");
        df.setDataStorageUsed(dataStorageConf);
        datafiles.add(df);
        url = new URL(Paths.get(baseStorageLocation.toString(), "file3.test").toString());
        df = new DataFile(url, "3", "MD5", DataType.RAWDATA, 50L, MimeType.valueOf("application/text"), aip,
                "file3.test");
        df.setDataStorageUsed(dataStorageConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
    }

    private void fillNearlineDataFileDb() throws MalformedURLException {
        AIP aip = getAIP();
        aipDao.save(aip);
        Set<DataFile> datafiles = Sets.newHashSet();
        URL url = new URL("staf://PLOP/Node/file10.test");
        DataFile df = new DataFile(url, "10", "MD5", DataType.RAWDATA, 50L, MimeType.valueOf("application/text"), aip,
                "file10.test");
        df.setDataStorageUsed(stafConf);
        datafiles.add(df);
        url = new URL("staf://PLOP/Node/file20.test");
        df = new DataFile(url, "20", "MD5", DataType.RAWDATA, 50L, MimeType.valueOf("application/text"), aip,
                "file20.test");
        df.setDataStorageUsed(stafConf);
        datafiles.add(df);
        url = new URL("staf://PLOP/Node/file30.test");
        df = new DataFile(url, "30", "MD5", DataType.RAWDATA, 50L, MimeType.valueOf("application/text"), aip,
                "file30.test");
        df.setDataStorageUsed(stafConf);
        datafiles.add(df);
        dataFileDao.save(datafiles);
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(EntityType.DATA,
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1)
                        .toString(),
                null);

        // Build IO
        InformationObjectBuilder ioBuilder = new InformationObjectBuilder();

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        ioBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, new URL("file", "", path));
        ioBuilder.getContentInformationBuilder().setSyntax("text", "description", "text/plain");

        ioBuilder.getPDIBuilder().setAccessRightInformation("publisherDID", "publisherID", "public");
        ioBuilder.getPDIBuilder().setFixityInformation("de89a907d33a9716d11765582102b2e0", "MD5");
        ioBuilder.getPDIBuilder().setProvenanceInformation("CS");
        ioBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                OffsetDateTime.now());

        aipBuilder.addInformationObjects(ioBuilder.build());
        AIP aip = aipBuilder.build();
        aip.addEvent(EventType.SUBMISSION.name(), "submission");
        return aip;
    }

    @Test
    public void loadOnlineFilesTest() throws MalformedURLException {

        fillOnlineDataFileDb();

        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "1", "2", "3");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(response.getAlreadyAvailable().size() == 3);
        Assert.assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void loadNearlineFilesTest() throws MalformedURLException {

        fillNearlineDataFileDb();

        AvailabilityRequest request = new AvailabilityRequest(OffsetDateTime.now(), "10", "20", "30");
        AvailabilityResponse response = aipService.loadFiles(request);
        Assert.assertTrue(String.format("Invalid number of available files %d", response.getAlreadyAvailable().size()),
                          response.getAlreadyAvailable().isEmpty());
        Assert.assertTrue(String.format("Invalid number of error files %d", response.getAlreadyAvailable().size()),
                          response.getErrors().isEmpty());
    }

    @After
    public void cleanUp() throws URISyntaxException, IOException {
        dataFileDao.deleteAll();
        pluginRepo.deleteAll();
        cachedFileRepository.deleteAll();
        if (baseStorageLocation != null) {
            Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                    .forEach(File::delete);
        }
    }

}
