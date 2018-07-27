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
package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DispatchErrors;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.AIPMiscAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.PluginConfigurationIdentifiersWrapper;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;

/**
 * Test class for plugin {@link AIPMiscAllocationStrategyPlugin}
 * @author Sébastien Binda
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class })
@RegardsTransactional
@DirtiesContext // If not set, the plugin list from IPluginService is not upa
public class AIPMiscAllocationStrategyIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    private Collection<StorageDataFile> dataFiles;

    private StorageDataFile dataFile1;

    private StorageDataFile dataFile2;

    private static final String SESSION = "Session 1";

    @Before
    public void init() throws ModuleException, MalformedURLException {
        initPlugins();
        initDataFiles();
    }

    private PluginConfiguration initNearLinePluginConf(String pluginLabel) throws ModuleException {
        PluginMetaData nearlineMeta = PluginUtils
                .createPluginMetaData(NearlineDataStorageTestPlugin.class,
                                      NearlineDataStorageTestPlugin.class.getPackage().getName(),
                                      IDataStorage.class.getPackage().getName(),
                                      INearlineDataStorage.class.getPackage().getName());
        return pluginService.savePluginConfiguration(new PluginConfiguration(nearlineMeta, pluginLabel, null));
    }

    private PluginConfiguration initLocalPluginconf(String label)
            throws ModuleException, IOException, URISyntaxException {
        URL baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/LocalDataStorageIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000L).getParameters();
        PluginMetaData localDataStorageMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, IDataStorage.class.getPackage().getName(),
                                      IOnlineDataStorage.class.getPackage().getName(),
                                      LocalDataStorage.class.getPackage().getName());
        PluginConfiguration localStorageConf = new PluginConfiguration(localDataStorageMeta, label, parameters);
        return pluginService.savePluginConfiguration(localStorageConf);
    }

    private PluginConfiguration initAllocation(String label, Map<String, PluginConfigurationIdentifiersWrapper> mapping)
            throws ModuleException {
        // lets get a allocation
        PluginMetaData allocation = PluginUtils
                .createPluginMetaData(AIPMiscAllocationStrategyPlugin.class,
                                      AIPMiscAllocationStrategyPlugin.class.getPackage().getName(),
                                      AIPMiscAllocationStrategyPlugin.class.getPackage().getName());
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(AIPMiscAllocationStrategyPlugin.MAP_PLUGINID_PLUGINCONFID_PARAMETER, mapping)
                .getParameters();
        return pluginService.savePluginConfiguration(new PluginConfiguration(allocation, label, parameters));
    }

    private void initDataFiles() throws MalformedURLException {
        dataFiles = Sets.newHashSet();
        AIP aip = getAIP();

        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());

        dataFile1 = new StorageDataFile(Sets.newHashSet(new URL("file", "", "fichier1.json")), "checksum", "MD5",
                DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aip, aipSession, "fichier1", null);
        dataFiles.add(dataFile1);
        dataFile2 = new StorageDataFile(Sets.newHashSet(new URL("file", "", "fichier2.json")), "checksum2", "MD5",
                DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aip, aipSession, "fichier2", null);
        dataFiles.add(dataFile2);
    }

    private AIP getAIP() throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA, getDefaultTenant(),
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, getDefaultTenant(),
                sipId.getEntityId(), 1);

        AIPBuilder aipBuilder = new AIPBuilder(aipId, sipId, null, EntityType.DATA, SESSION);

        Path path = Paths.get(System.getProperty("user.dir"), "/src/test/resources/data.txt");
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, path, "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());
        String storageJson = "[{\"pluginId\":\"" + NearlineDataStorageTestPlugin.PLUGIN_ID
                + "\",\"directory\":\"dir1\"},{\"pluginId\":\"Local\",\"directory\":\"dir2\"}]";
        aipBuilder.addMiscInformation("storage", gson.fromJson(storageJson, Object.class));
        return aipBuilder.build();
    }

    protected void initPlugins() throws ModuleException, MalformedURLException {
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(AIPMiscAllocationStrategyPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(INearlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(NearlineDataStorageTestPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());
    }

    /**
     * Test nominal use case.
     * - Multiple storage defined in each AIP into the misc.storage properties
     * - FromSIPAllocationStrategyPlugin configured to select one configuration for the two plugin types
     * @throws ModuleException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testOkWithMultipleConfigurations() throws ModuleException, IOException, URISyntaxException {
        // Init two nearline plugin configurations
        PluginConfiguration nearConf1 = initNearLinePluginConf("label1");
        PluginConfiguration nearConf2 = initNearLinePluginConf("label2");
        // Init two Local plugin configurations
        PluginConfiguration localConf1 = initLocalPluginconf("label3");
        PluginConfiguration localConf2 = initLocalPluginconf("label4");
        // Init allocation strategy
        Map<String, PluginConfigurationIdentifiersWrapper> mapping = Maps.newHashMap();
        mapping.put(NearlineDataStorageTestPlugin.PLUGIN_ID,
                    new PluginConfigurationIdentifiersWrapper(Arrays.asList(nearConf1.getId())));
        mapping.put("Local", new PluginConfigurationIdentifiersWrapper(Arrays.asList(localConf1.getId())));
        PluginConfiguration allocationStrategyConf = initAllocation("allocation", mapping);

        AIPMiscAllocationStrategyPlugin allocationStrategy = pluginService.getPlugin(allocationStrategyConf.getId());
        Multimap<Long, StorageDataFile> result = allocationStrategy.dispatch(dataFiles, new DispatchErrors());
        Assert.assertTrue(result.containsEntry(nearConf1.getId(), dataFile1));
        Assert.assertFalse(result.containsKey(nearConf2.getId()));
        Assert.assertTrue(result.containsEntry(localConf1.getId(), dataFile1));
        Assert.assertTrue(result.containsEntry(nearConf1.getId(), dataFile2));
        Assert.assertFalse(result.containsKey(localConf2.getId()));
        Assert.assertTrue(result.containsEntry(localConf1.getId(), dataFile2));
    }

    /**
     * Test nominal use case.
     * - Multiple storage defined in each AIP into the misc.storage properties
     * - FromSIPAllocationStrategyPlugin configured to select one configuration for the one plugin type
     * - As no plugin configuration identifier is defined for the scond plugin type, the dataFiles are not dispatch to
     * be store with it.
     * @throws ModuleException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testPartialDispatchWithMultipleConfigurations()
            throws ModuleException, IOException, URISyntaxException {
        // Init two nearline plugin configurations
        PluginConfiguration nearConf1 = initNearLinePluginConf("label1");
        PluginConfiguration nearConf2 = initNearLinePluginConf("label2");
        // Init two Local plugin configurations
        PluginConfiguration localConf1 = initLocalPluginconf("label3");
        PluginConfiguration localConf2 = initLocalPluginconf("label4");
        // Init allocation strategy
        Map<String, PluginConfigurationIdentifiersWrapper> mapping = Maps.newHashMap();
        mapping.put(NearlineDataStorageTestPlugin.PLUGIN_ID,
                    new PluginConfigurationIdentifiersWrapper(Arrays.asList(nearConf1.getId())));
        PluginConfiguration allocationStrategyConf = initAllocation("allocation", mapping);

        AIPMiscAllocationStrategyPlugin allocationStrategy = pluginService.getPlugin(allocationStrategyConf.getId());
        Multimap<Long, StorageDataFile> result = allocationStrategy.dispatch(dataFiles, new DispatchErrors());
        Assert.assertTrue(result.containsEntry(nearConf1.getId(), dataFile1));
        Assert.assertTrue(result.containsEntry(nearConf1.getId(), dataFile2));
        Assert.assertFalse(result.containsKey(nearConf2.getId()));
        Assert.assertFalse(result.containsKey(localConf1.getId()));
        Assert.assertFalse(result.containsKey(localConf2.getId()));
    }

    /**
     * Test nominal use case.
     * - Multiple storage defined in each AIP into the misc.storage properties
     * - FromSIPAllocationStrategyPlugin not configured
     * - As there is only one configuration for each plugin type, each file is dispatched to be stored using the only
     * one configuration.
     * @throws ModuleException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testOkWithoutConfiguration() throws ModuleException, IOException, URISyntaxException {
        // Init one nearline plugin configurations
        PluginConfiguration nearConf1 = initNearLinePluginConf("label1");
        // Init one Local plugin configurations
        PluginConfiguration localConf1 = initLocalPluginconf("label3");
        // Init allocation strategy
        PluginConfiguration allocationStrategyConf = initAllocation("allocation", null);

        AIPMiscAllocationStrategyPlugin allocationStrategy = pluginService.getPlugin(allocationStrategyConf.getId());
        Multimap<Long, StorageDataFile> result = allocationStrategy.dispatch(dataFiles, new DispatchErrors());
        Assert.assertTrue(result.containsEntry(nearConf1.getId(), dataFile1));
        Assert.assertTrue(result.containsEntry(localConf1.getId(), dataFile1));
        Assert.assertTrue(result.containsEntry(nearConf1.getId(), dataFile2));
        Assert.assertTrue(result.containsEntry(localConf1.getId(), dataFile2));
    }

    /**
     * Check that the files ared not dispatched if there is many configuration for a pluginId and no
     * configuration is set to the allocation plugin strategy to define wich one to use.
     * @throws ModuleException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testPartialDispatchWithoutConfiguration() throws ModuleException, IOException, URISyntaxException {
        // Init two nealine plugin configurations
        PluginConfiguration nearConf1 = initNearLinePluginConf("label1");
        PluginConfiguration nearConf2 = initNearLinePluginConf("label2");
        // Init two Local plugin configurations
        PluginConfiguration localConf = initLocalPluginconf("label3");
        // Init allocation strategy
        PluginConfiguration allocationStrategyConf = initAllocation("allocation", null);

        AIPMiscAllocationStrategyPlugin allocationStrategy = pluginService.getPlugin(allocationStrategyConf.getId());
        Multimap<Long, StorageDataFile> result = allocationStrategy.dispatch(dataFiles, new DispatchErrors());
        Assert.assertFalse(result.containsKey(nearConf1.getId()));
        Assert.assertFalse(result.containsKey(nearConf2.getId()));
        Assert.assertTrue(result.containsEntry(localConf.getId(), dataFile1));
        Assert.assertTrue(result.containsEntry(localConf.getId(), dataFile2));
    }

    /**
     * Check that the files ared not dispatched if there is many configuration for a pluginId and no
     * configuration is set to the allocation plugin strategy to define wich one to use.
     * @throws ModuleException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testKOWithoutAnyConfiguration() throws ModuleException, IOException, URISyntaxException {
        // Init two nearline plugin configurations
        initNearLinePluginConf("label1");
        initNearLinePluginConf("label2");
        // Init two Local plugin configurations
        initLocalPluginconf("label3");
        initLocalPluginconf("label4");
        // Init allocation strategy
        PluginConfiguration allocationStrategyConf = initAllocation("allocation", null);

        AIPMiscAllocationStrategyPlugin allocationStrategy = pluginService.getPlugin(allocationStrategyConf.getId());
        Multimap<Long, StorageDataFile> result = allocationStrategy.dispatch(dataFiles, new DispatchErrors());
        Assert.assertTrue(result.isEmpty());
    }
}
