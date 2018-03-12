/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

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
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.PropertyDataStorageMapping;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.PropertyMappingAllocationStrategy;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class })
@RegardsTransactional
public class PropertyMappingAllocationStrategyIT extends AbstractRegardsServiceTransactionalIT {

    private static final String JSON_PATH = "$.properties.pdi.provenanceInformation.additional.property";

    private static final String PROPERTY_VALUE = "value";

    private static final String PROPERTY_MAPPING_ALLOC_STRAT_LABEL = "PROPERTY_MAPPING_ALLOC_STRAT_LABEL";

    private static final String LOCAL_STORAGE_LABEL = "LOCAL_DATA_STORAGE_LABEL";

    private Long mappedDataStorageConfId;

    private Collection<StorageDataFile> dataFiles;

    private PluginConfiguration propertyMappingAllocStratConf;

    private StorageDataFile propertyDataFile;

    private StorageDataFile otherDataFile;

    private StorageDataFile propertyWrongValDataFile;

    @Autowired
    private IPluginService pluginService;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        initPlugin();
        initDataFiles();
    }

    private void initDataFiles() throws MalformedURLException {
        dataFiles = Sets.newHashSet();
        // lets get an aip and add it the proper property
        AIP aipWithProperty = getAIP();
        AIPBuilder builder = new AIPBuilder(aipWithProperty);
        builder.getPDIBuilder().addAdditionalProvenanceInformation("property", PROPERTY_VALUE);
        propertyDataFile = new StorageDataFile(Sets.newHashSet(new URL("file", "", "truc.json")), "checksum", "MD5",
                DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aipWithProperty, "truc", null);
        dataFiles.add(propertyDataFile);
        AIP aipWithoutProperty = getAIP();
        otherDataFile = new StorageDataFile(Sets.newHashSet(new URL("file", "", "local.json")), "checksum2", "MD5",
                DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aipWithoutProperty, "local", null);
        dataFiles.add(otherDataFile);
        AIP aipWithPropertyWrongVal = getAIP();
        builder = new AIPBuilder(aipWithPropertyWrongVal);
        builder.getPDIBuilder().addAdditionalProvenanceInformation("property", PROPERTY_VALUE + 3);
        propertyWrongValDataFile = new StorageDataFile(Sets.newHashSet(new URL("file", "", "truc.json")), "checksum3",
                "MD5", DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aipWithPropertyWrongVal, "truc", null);
        dataFiles.add(propertyWrongValDataFile);
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1),
                null, EntityType.DATA);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", "text/plain");
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());

        return aipBuilder.build();
    }

    private void initPlugin() throws ModuleException, IOException, URISyntaxException {
        pluginService.addPluginPackage(PropertyMappingAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(IOnlineDataStorage.class.getPackage().getName());
        pluginService.addPluginPackage(LocalDataStorage.class.getPackage().getName());

        URL baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/LocalDataStorageIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000L).getParameters();
        PluginMetaData localDataStorageMeta = PluginUtils
                .createPluginMetaData(LocalDataStorage.class, IDataStorage.class.getPackage().getName(),
                                      IOnlineDataStorage.class.getPackage().getName(),
                                      LocalDataStorage.class.getPackage().getName());
        PluginConfiguration localStorageConf = new PluginConfiguration(localDataStorageMeta, LOCAL_STORAGE_LABEL,
                parameters);
        localStorageConf = pluginService.savePluginConfiguration(localStorageConf);

        mappedDataStorageConfId = localStorageConf.getId();

        PluginMetaData propertyMappingAllocStratMeta = PluginUtils
                .createPluginMetaData(PropertyMappingAllocationStrategy.class,
                                      PropertyMappingAllocationStrategy.class.getPackage().getName(),
                                      IAllocationStrategy.class.getPackage().getName());
        // before getting the alloc strat plg params, lets make some mapping
        Set<PropertyDataStorageMapping> mappings = Sets.newHashSet();
        mappings.add(new PropertyDataStorageMapping(PROPERTY_VALUE, mappedDataStorageConfId));
        List<PluginParameter> propertyMappingAllocStratParam = PluginParametersFactory.build()
                .addParameter(PropertyMappingAllocationStrategy.PROPERTY_PATH, JSON_PATH)
                .addParameter(PropertyMappingAllocationStrategy.PROPERTY_VALUE_DATA_STORAGE_MAPPING, mappings)
                .addParameter(PropertyMappingAllocationStrategy.QUICKLOOK_DATA_STORAGE_CONFIGURATION_ID,
                              mappedDataStorageConfId)
                .getParameters();
        propertyMappingAllocStratConf = new PluginConfiguration(propertyMappingAllocStratMeta,
                PROPERTY_MAPPING_ALLOC_STRAT_LABEL, propertyMappingAllocStratParam);
        propertyMappingAllocStratConf = pluginService.savePluginConfiguration(propertyMappingAllocStratConf);
    }

    @Test
    public void testOk() throws ModuleException {
        PropertyMappingAllocationStrategy allocStrat = pluginService.getPlugin(propertyMappingAllocStratConf.getId());
        Multimap<Long, StorageDataFile> result = allocStrat.dispatch(dataFiles);
        Assert.assertTrue("dispatch should have mapped propertyDataFile to the data storage conf id",
                          result.containsEntry(mappedDataStorageConfId, propertyDataFile));
        Assert.assertFalse("dispatch should not have mapped otherDataFile to any data storage conf id",
                           result.containsValue(otherDataFile));
        Assert.assertFalse("dispatch should not have mapped propertyWrongValDataFile to any data storage conf id",
                           result.containsValue(propertyWrongValDataFile));
    }
}
