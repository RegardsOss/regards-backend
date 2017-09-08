package fr.cnes.regards.modules.storage.plugins.local;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.MimeType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.Event;
import fr.cnes.regards.modules.storage.domain.EventType;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.ProgressManager;
import fr.cnes.regards.modules.storage.plugin.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.local.LocalWorkingSubset;
import fr.cnes.regards.plugins.utils.PluginUtils;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class })
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class LocalDataStorageIT extends AbstractRegardsServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDataStorageIT.class);

    private static final String LOCAL_STORAGE_LABEL = "LocalDataStorageIT";

    @Autowired
    Environment env;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private PluginConfiguration localStorageConf;

    private URL baseStorageLocation;

    @BeforeTransaction
    public void setTenant() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
    }

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/LocalDataStorageIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                              gson.toJson(baseStorageLocation)).getParameters();
        //new plugin conf for LocalDataStorage storage into target/LocalDataStorageIT
        PluginMetaData localStorageMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, LocalDataStorage.class.getPackage().getName(),
                                      IDataStorage.class.getPackage().getName());
        localStorageConf = new PluginConfiguration(localStorageMeta, LOCAL_STORAGE_LABEL, parameters);
        localStorageConf = pluginService.savePluginConfiguration(localStorageConf);
    }

    @Test
    @Ignore("This test is just here to see if we gain some time or not with parallelism")
    public void testParallelGain() throws IOException, ModuleException {
        // for test purpose, lets see minimum value to gain time with parallelStream just transferring a file with no verification
        AIP aip = getAipFromFile();
        String jsonAip = gson.toJson(aip);
        List<String> groupToWrite = Lists.newArrayList();
        int parallelSize = 100;
        for (int i = 0; i < parallelSize; i++) {
            groupToWrite.add(jsonAip);
        }
        String sequentialLocation = baseStorageLocation.getPath() + "/sequential";
        Files.createDirectories(Paths.get(sequentialLocation));
        int i = 0;
        //add timer
        LocalTime startTime = LocalTime.now();
        for (String toWrite : groupToWrite) {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(sequentialLocation, "aip" + i++ + ".json"));
            writer.write(toWrite);
            writer.flush();
            writer.close();
        }
        //get timer
        LocalTime endTime = LocalTime.now();
        Duration spent = Duration.between(startTime, endTime);
        LOG.info("#################################################");
        LOG.info("############# Sequential storage took: " + spent.getSeconds() + " seconds and "
                         + spent.getNano() / 1_000_000 + " millis");
        LOG.info("#################################################");
        //lets reset the timer
        startTime = LocalTime.now();
        String parallelLocation = baseStorageLocation.getPath() + "/parallel";
        Files.createDirectories(Paths.get(parallelLocation));
        groupToWrite.parallelStream().forEach(toWrite -> {
            try {
                BufferedWriter writer = Files
                        .newBufferedWriter(Paths.get(parallelLocation, "aip" + Math.random() + ".json"));
                writer.write(toWrite);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        endTime = LocalTime.now();
        spent = Duration.between(startTime, endTime);
        LOG.info("#################################################");
        LOG.info("############# Parallel storage took: " + spent.getSeconds() + " seconds and "
                         + spent.getNano() / 1_000_000 + " millis");
        LOG.info("#################################################");
    }

    @Test
    public void testStore() throws ModuleException, IOException {
        ProgressManager progressManager = Mockito.mock(ProgressManager.class);
        AIP aip = getAipFromFile();
        aip.addEvent(new Event("just for fun", OffsetDateTime.now(), EventType.SUBMISSION));
        LocalDataStorage storagePlugin = pluginService.getPlugin(localStorageConf.getId());
        // valid file to get a call to progressManager.storageSucceed
        DataFile validDF = new DataFile(
                new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data.txt"),
                "538b3f98063b77e50f78b51f1a6acd8c", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"), aip);
        // file that does not exist to get a call to progressManager.storageFailed
        DataFile ghostDF = new DataFile(new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data_do_not_exist.txt"),
                                          "unknown", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"), aip);
        // invalid checksum to check call to progressManager.storageFailed
        DataFile invalidDF = new DataFile(
                new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data.txt"),
                "01234567890123456789012345678901", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"), aip);
        Set<DataFile> dataFiles = Sets.newHashSet(validDF, ghostDF, invalidDF);
        LocalWorkingSubset workingSubSet = new LocalWorkingSubset(dataFiles);
        storagePlugin.store(workingSubSet, false, progressManager);
        Mockito.verify(progressManager).storageSucceed(Mockito.eq(validDF), Mockito.any());
        Mockito.verify(progressManager, Mockito.times(2)).storageFailed(Mockito.any(), Mockito.any());
    }

    private AIP getAipFromFile() throws IOException {
        FileReader fr = new FileReader("src/test/resources/aip_sample.json");
        BufferedReader br = new BufferedReader(fr);
        String fileLine = br.readLine();
        AIP aip = gson.fromJson(fileLine, AIP.class);
        br.close();
        fr.close();
        return aip;
    }

    @After
    public void cleanUp() throws ModuleException, URISyntaxException, IOException {
        pluginService.deletePluginConfiguration(localStorageConf.getId());
        Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .forEach(File::delete);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
