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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

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
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DispatchErrors;
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

    private static final String SESSION = "Session 1";

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
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());

        AIPBuilder builder = new AIPBuilder(aipWithProperty);
        builder.getPDIBuilder().addAdditionalProvenanceInformation("property", PROPERTY_VALUE);
        propertyDataFile = new StorageDataFile(Sets.newHashSet(new URL("file", "", "truc.json")), "checksum", "MD5",
                DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aipWithProperty, aipSession, "truc", null);
        dataFiles.add(propertyDataFile);
        AIP aipWithoutProperty = getAIP();
        otherDataFile = new StorageDataFile(Sets.newHashSet(new URL("file", "", "local.json")), "checksum2", "MD5",
                DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aipWithoutProperty, aipSession, "local", null);
        dataFiles.add(otherDataFile);
        AIP aipWithPropertyWrongVal = getAIP();
        builder = new AIPBuilder(aipWithPropertyWrongVal);
        builder.getPDIBuilder().addAdditionalProvenanceInformation("property", PROPERTY_VALUE + 3);
        propertyWrongValDataFile = new StorageDataFile(Sets.newHashSet(new URL("file", "", "truc.json")), "checksum3",
                "MD5", DataType.OTHER, 666L, MediaType.APPLICATION_JSON, aipWithPropertyWrongVal, aipSession, "truc",
                null);
        dataFiles.add(propertyWrongValDataFile);
    }

    private AIP getAIP() throws MalformedURLException {

        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA, getDefaultTenant(),
                UUID.randomUUID(), 1);
        UniformResourceName aipId = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, getDefaultTenant(),
                sipId.getEntityId(), 1);
        AIPBuilder aipBuilder = new AIPBuilder(aipId, Optional.of(sipId), "providerId", EntityType.DATA, SESSION);

        Path path = Paths.get(System.getProperty("user.dir"), "/src/test/resources/data.txt");
        aipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, path, "MD5",
                                                                "de89a907d33a9716d11765582102b2e0");
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                                                                 OffsetDateTime.now());

        return aipBuilder.build();
    }

    private void initPlugin() throws ModuleException, IOException, URISyntaxException {

        URL baseStorageLocation = new URL("file", "", System.getProperty("user.dir") + "/target/LocalDataStorageIT");
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000L).getParameters();
        PluginMetaData localDataStorageMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        PluginConfiguration localStorageConf = new PluginConfiguration(localDataStorageMeta, LOCAL_STORAGE_LABEL,
                parameters);
        localStorageConf = pluginService.savePluginConfiguration(localStorageConf);

        mappedDataStorageConfId = localStorageConf.getId();

        PluginMetaData propertyMappingAllocStratMeta = PluginUtils
                .createPluginMetaData(PropertyMappingAllocationStrategy.class);
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
        Multimap<Long, StorageDataFile> result = allocStrat.dispatch(dataFiles, new DispatchErrors());
        Assert.assertTrue("dispatch should have mapped propertyDataFile to the data storage conf id",
                          result.containsEntry(mappedDataStorageConfId, propertyDataFile));
        Assert.assertFalse("dispatch should not have mapped otherDataFile to any data storage conf id",
                           result.containsValue(otherDataFile));
        Assert.assertFalse("dispatch should not have mapped propertyWrongValDataFile to any data storage conf id",
                           result.containsValue(propertyWrongValDataFile));
    }
}
