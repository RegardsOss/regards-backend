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
package fr.cnes.regards.modules.storage.plugins.datastorage.local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class })
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class LocalDataStorageIT extends AbstractRegardsServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDataStorageIT.class);

    private static final String LOCAL_STORAGE_LABEL = "LocalDataStorageIT";

    private static final String SESSION = "SESSION 1";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Gson gson;

    private PluginConfiguration localStorageConf;

    private URL baseStorageLocation;

    @BeforeTransaction
    public void setTenant() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/LocalDataStorageIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()),
                     IPluginParam.build(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000L));
        // new plugin conf for LocalDataStorage storage into target/LocalDataStorageIT
        PluginMetaData localStorageMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        localStorageConf = new PluginConfiguration(localStorageMeta, LOCAL_STORAGE_LABEL, parameters);
        localStorageConf = pluginService.savePluginConfiguration(localStorageConf);
    }

    @Test
    @Ignore("This test is just here to see if we gain some time or not with parallelism")
    public void testParallelGain() throws IOException, ModuleException {
        // for test purpose, lets see minimum value to gain time with parallelStream just transferring a file with no
        // verification
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
        // add timer
        LocalTime startTime = LocalTime.now();
        for (String toWrite : groupToWrite) {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(sequentialLocation, "aip" + i++ + ".json"));
            writer.write(toWrite);
            writer.flush();
            writer.close();
        }
        // get timer
        LocalTime endTime = LocalTime.now();
        Duration spent = Duration.between(startTime, endTime);
        LOG.info("#################################################");
        LOG.info("############# Sequential storage took: " + spent.getSeconds() + " seconds and "
                + spent.getNano() / 1_000_000 + " millis");
        LOG.info("#################################################");
        // lets reset the timer
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
    public void testStore() throws ModuleException, IOException, NotAvailablePluginConfigurationException {
        IProgressManager progressManager = Mockito.mock(IProgressManager.class);
        AIP aip = getAipFromFile();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aip.addEvent(EventType.SUBMISSION.name(), "just for fun", OffsetDateTime.now());
        LocalDataStorage storagePlugin = pluginService.getPlugin(localStorageConf.getBusinessId());
        // valid file to get a call to progressManager.storageSucceed
        StorageDataFile validDF = new StorageDataFile(
                Sets.newHashSet(new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data.txt")),
                "538b3f98063b77e50f78b51f1a6acd8c", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"),
                new AIPEntity(aip, aipSession), "data.txt", null);
        // valid file to get a call to progressManager.storageSucceed
        StorageDataFile validDFWithProvidedDirectory = new StorageDataFile(
                Sets.newHashSet(new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data2.txt")),
                "36fc2e44e266cd50f1f9dbc5b9767138", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"),
                new AIPEntity(aip, aipSession), "data2.txt", "/provided/directory");
        // file that does not exist to get a call to progressManager.storageFailed
        StorageDataFile ghostDF = new StorageDataFile(
                Sets.newHashSet(new URL("file", "",
                        System.getProperty("user.dir") + "/src/test/resources/data_do_not_exist.txt")),
                "unknown", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"), new AIPEntity(aip, aipSession),
                "data_do_not_exist.txt", null);
        // invalid checksum to check call to progressManager.storageFailed
        StorageDataFile invalidDF = new StorageDataFile(
                Sets.newHashSet(new URL("file", "", System.getProperty("user.dir") + "/src/test/resources/data.txt")),
                "01234567890123456789012345678901", "MD5", DataType.RAWDATA, 123L, new MimeType("text", "plain"),
                new AIPEntity(aip, aipSession), "data.txt", null);
        Set<StorageDataFile> dataFiles = Sets.newHashSet(validDF, validDFWithProvidedDirectory, ghostDF, invalidDF);
        LocalWorkingSubset workingSubSet = new LocalWorkingSubset(dataFiles);
        storagePlugin.store(workingSubSet, false, progressManager);
        Mockito.verify(progressManager).storageSucceed(Mockito.eq(validDF), Mockito.any(), Mockito.any());
        Mockito.verify(progressManager).storageSucceed(Mockito.eq(validDFWithProvidedDirectory), Mockito.any(),
                                                       Mockito.any());
        Mockito.verify(progressManager, Mockito.times(2)).storageFailed(Mockito.any(), Mockito.any(), Mockito.any());

        // Check files are stored
        Path pathToCheck = Paths.get(baseStorageLocation.getPath(), validDFWithProvidedDirectory.getStorageDirectory(),
                                     validDFWithProvidedDirectory.getChecksum());
        LOG.info("Checking file {}", pathToCheck.toString());
        Assert.assertTrue(Files.exists(pathToCheck));
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
        pluginService.deletePluginConfiguration(localStorageConf.getBusinessId());
        Files.walk(Paths.get(baseStorageLocation.toURI())).sorted(Comparator.reverseOrder()).map(Path::toFile)
                .forEach(File::delete);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
