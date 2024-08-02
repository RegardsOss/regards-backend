/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.jsoniter.property.JsoniterAttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.entities.DatasetConfiguration;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.model.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.model.dao.IModelAttrAssocRepository;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.dao.IRestrictionRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.model.service.IAttributeModelService;
import fr.cnes.regards.modules.model.service.IModelService;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * @author Marc SORDI
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dam_configuration" },
                    locations = "classpath:es.properties")
public class DamConfigurationManagerIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamConfigurationManagerIT.class);

    private static final Path DATA_FOLDER = Paths.get("src", "test", "resources", "data");

    @Autowired
    private DamConfigurationManager configurationManager;

    @Autowired
    private IModelService modelService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IModelRepository modelRepository;

    @Autowired
    private IModelAttrAssocRepository modelAttrAssocRepository;

    @Autowired
    private IAttributeModelRepository attributeModelRepository;

    @Autowired
    private IAttributeModelService attributeModelService;

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    @Autowired
    protected JsoniterAttributeModelPropertyTypeFinder jsoniterAttributeFactory;

    @Autowired
    private IDatasetRepository datasetRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IRestrictionRepository restrictionRepository;

    private Model datasetModel;

    private Model dataModel;

    private PluginConfiguration datasourceConfiguration;

    @Before
    public void before() throws ModuleException, IOException {
        // Clean all
        datasetRepository.deleteAllInBatch();
        pluginConfigurationRepository.deleteAllInBatch();
        modelAttrAssocRepository.deleteAllInBatch();
        attributeModelRepository.deleteAll();
        modelRepository.deleteAll();
        restrictionRepository.deleteAll();

        // - Prepare context
        // Import dataset model
        datasetModel = modelService.importModel(Files.newInputStream(DATA_FOLDER.resolve("dataset-model.xml")));
        // Import data model
        dataModel = modelService.importModel(Files.newInputStream(DATA_FOLDER.resolve("data-model.xml")));
        // Refresh attribute factory
        List<AttributeModel> atts = attributeModelService.getAttributes(null, null, null);
        gsonAttributeFactory.refresh(getDefaultTenant(), atts);
        jsoniterAttributeFactory.refresh(getDefaultTenant(), atts);

        // Import datasource
        datasourceConfiguration = PluginConfiguration.build("FakeDatasourcePlugin",
                                                            "Test datasource",
                                                            IPluginParam.set(IPluginParam.build(FakeDatasourcePlugin.MODEL_PARAM,
                                                                                                dataModel.getName())));
        pluginService.savePluginConfiguration(datasourceConfiguration);
    }

    @Test
    public void importConfiguration() throws IOException, ModuleException {

        DatasetFeature feature = new DatasetFeature(getDefaultTenant(), "dataset01", "Dataset 01");
        feature.setId(null);
        feature.setModel(datasetModel.getName());
        // Add required properties
        feature.addProperty(IProperty.buildString("description", "Dataset 01 description"));
        feature.addProperty(IProperty.buildString("license", "Proprietary"));
        // Build providers
        JsonArray providers = new JsonArray();
        // - First provider
        JsonObject firstProvider = new JsonObject();
        firstProvider.addProperty("name", "CNES");
        firstProvider.addProperty("description", "Centre National ...");
        JsonArray roles = new JsonArray();
        roles.add("licensor");
        roles.add("producer");
        firstProvider.add("roles", roles);
        firstProvider.addProperty("url", "http://www.cnes.fr");
        // - Add provider(s)
        providers.add(firstProvider);
        feature.addProperty(IProperty.buildJson("providers", providers));

        // Create a dataset configuration module item
        ModuleConfigurationItem<DatasetConfiguration> item = ModuleConfigurationItem.build(new DatasetConfiguration(
            datasourceConfiguration.getBusinessId(),
            "hydro" + ".data_type:\"TEST_TYPE\"",
            feature));

        // Import configuration
        ModuleConfiguration conf = ModuleConfiguration.build(configurationManager.getModuleInformation(),
                                                             Lists.newArrayList(item));
        Set<String> errors = configurationManager.importConfiguration(conf, Sets.newHashSet());
        errors.forEach(error -> LOGGER.error(error));
        Assert.assertTrue("Error detected", errors.isEmpty());

        // Re-entrant import
        errors = configurationManager.importConfiguration(conf, Sets.newHashSet());
        errors.forEach(error -> LOGGER.error(error));
        Assert.assertTrue("Error detected", errors.isEmpty());

        // Export configuration
        ModuleConfiguration moduleConfiguration = configurationManager.exportConfiguration();
        Assert.assertNotNull("Module configuration must not be null", moduleConfiguration);
    }
}
