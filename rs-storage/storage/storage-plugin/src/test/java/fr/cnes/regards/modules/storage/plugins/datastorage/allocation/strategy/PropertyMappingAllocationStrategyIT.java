package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.IAllocationStrategy;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.PropertyDataStorageMapping;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.PropertyMappingAllocationStrategy;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@ContextConfiguration(classes = { MockingResourceServiceConfiguration.class })
@RegardsTransactional
public class PropertyMappingAllocationStrategyIT extends AbstractRegardsServiceTransactionalIT {

    private static final String JSON_PATH = "$.properties.pdi.provenanceInformation.additional.property";

    private static final String PROPERTY_VALUE = "value";

    private static final Long MAPPED_DATA_STORAGE_CONF_ID = 1L;

    private static final String PROPERTY_MAPPING_ALLOC_STRAT_LABEL = "PROPERTY_MAPPING_ALLOC_STRAT_LABEL";

    private Collection<DataFile> dataFiles;

    private PluginConfiguration propertyMappingAllocStratConf;

    private DataFile propertyDataFile;

    private DataFile otherDataFile;

    private DataFile propertyWrongValDataFile;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Before
    public void init() throws MalformedURLException, ModuleException {
        initPlugin();
        initDataFiles();
    }

    private void initDataFiles() throws MalformedURLException {
        dataFiles = Sets.newHashSet();
        //lets get an aip and add it the proper property
        AIP aipWithProperty = getAIP();
        AIPBuilder builder = new AIPBuilder(aipWithProperty);
        builder.getPDIBuilder().addAdditionalProvenanceInformation("property", PROPERTY_VALUE);
        propertyDataFile = new DataFile(new URL("file", "", "truc.json"),
                                        "checksum",
                                        "MD5",
                                        DataType.OTHER,
                                        666L,
                                        MediaType.APPLICATION_JSON,
                                        aipWithProperty,
                                        "truc");
        dataFiles.add(propertyDataFile);
        AIP aipWithoutProperty = getAIP();
        otherDataFile = new DataFile(new URL("file", "", "local.json"),
                                     "checksum2",
                                     "MD5",
                                     DataType.OTHER,
                                     666L,
                                     MediaType.APPLICATION_JSON,
                                     aipWithoutProperty,
                                     "local");
        dataFiles.add(otherDataFile);
        AIP aipWithPropertyWrongVal = getAIP();
        builder = new AIPBuilder(aipWithPropertyWrongVal);
        builder.getPDIBuilder().addAdditionalProvenanceInformation("property", PROPERTY_VALUE + 3);
        propertyWrongValDataFile = new DataFile(new URL("file", "", "truc.json"),
                                                "checksum3",
                                                "MD5",
                                                DataType.OTHER,
                                                666L,
                                                MediaType.APPLICATION_JSON,
                                                aipWithPropertyWrongVal,
                                                "truc");
        dataFiles.add(propertyWrongValDataFile);
    }

    private AIP getAIP() throws MalformedURLException {

        AIPBuilder aipBuilder = new AIPBuilder(new UniformResourceName(OAISIdentifier.AIP,
                                                                       EntityType.DATA,
                                                                       DEFAULT_TENANT,
                                                                       UUID.randomUUID(),
                                                                       1), null, EntityType.DATA);

        String path = System.getProperty("user.dir") + "/src/test/resources/data.txt";
        aipBuilder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, new URL("file", "", path), "MD5", "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", "text/plain");
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder()
                .addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event", OffsetDateTime.now());

        return aipBuilder.build();
    }

    private void initPlugin() throws ModuleException {
        pluginService.addPluginPackage(PropertyMappingAllocationStrategy.class.getPackage().getName());
        pluginService.addPluginPackage(IAllocationStrategy.class.getPackage().getName());
        PluginMetaData propertyMappingAllocStratMeta = PluginUtils.createPluginMetaData(
                PropertyMappingAllocationStrategy.class,
                PropertyMappingAllocationStrategy.class.getPackage().getName(),
                IAllocationStrategy.class.getPackage().getName());
        //before getting the alloc strat plg params, lets make some mapping
        Set<PropertyDataStorageMapping> mappings = Sets.newHashSet();
        mappings.add(new PropertyDataStorageMapping(PROPERTY_VALUE, MAPPED_DATA_STORAGE_CONF_ID));
        List<PluginParameter> propertyMappingAllocStratParam = PluginParametersFactory.build()
                .addParameter(PropertyMappingAllocationStrategy.PROPERTY_PATH, JSON_PATH).addParameter(
                        PropertyMappingAllocationStrategy.PROPERTY_VALUE_DATA_STORAGE_MAPPING,
                        gson.toJson(mappings)).getParameters();
        propertyMappingAllocStratConf = new PluginConfiguration(propertyMappingAllocStratMeta,
                                                                PROPERTY_MAPPING_ALLOC_STRAT_LABEL,
                                                                propertyMappingAllocStratParam);
        propertyMappingAllocStratConf = pluginService.savePluginConfiguration(propertyMappingAllocStratConf);
    }

    @Test
    public void testOk() throws ModuleException {
        PropertyMappingAllocationStrategy allocStrat = pluginService.getPlugin(propertyMappingAllocStratConf.getId());
        Multimap<Long, DataFile> result = allocStrat.dispatch(dataFiles);
        Assert.assertTrue("dispatch should have mapped propertyDataFile to the data storage conf id",
                          result.containsEntry(MAPPED_DATA_STORAGE_CONF_ID, propertyDataFile));
        Assert.assertFalse("dispatch should not have mapped otherDataFile to any data storage conf id",
                           result.containsValue(otherDataFile));
        Assert.assertFalse("dispatch should not have mapped propertyWrongValDataFile to any data storage conf id",
                           result.containsValue(propertyWrongValDataFile));
    }
}
