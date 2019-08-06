/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.DataStorageEventHandler;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
public class PrioritizedDataStorageControllerIT extends AbstractRegardsTransactionalIT {

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

    private static final String DATA_STORAGE_CONF_LABEL_1 = "PrioritizedDataStorageControllerIT_1";

    private static final String DATA_STORAGE_CONF_LABEL_2 = "PrioritizedDataStorageControllerIT_2";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IDataFileDao dataFileDao;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Test
    public void testCreate() throws IOException, URISyntaxException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        URL baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L),
                     IPluginParam.build(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()));
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, DATA_STORAGE_CONF_LABEL_1,
                parameters, 0);
        PrioritizedDataStorage toCreate = new PrioritizedDataStorage(dataStorageConf, null, DataStorageType.ONLINE);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        requestBuilderCustomizer
                .expect(MockMvcResultMatchers.jsonPath("$.content.id", Matchers.notNullValue(Long.class)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.dataStorageType",
                                                                       Matchers.is(DataStorageType.ONLINE.name())));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.priority", Matchers.is(0)));
        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Prioritized data storage")), documentPrioritizedDataStorageRequestBody(true)));
        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedResponseFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Prioritized data storage")), documentPrioritizedDataStorageResponseBody()));
        performDefaultPost(PrioritizedDataStorageController.BASE_PATH, toCreate, requestBuilderCustomizer,
                           "Could not create a PrioritizedDataStorage");
    }

    @Test
    public void testRetrieve() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.documentPathParameters(RequestDocumentation.parameterWithName("id")
                .description("the prioritized data storage id")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Number"), Attributes
                        .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Should be a whole number")));
        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedResponseFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Prioritized data storage")), documentPrioritizedDataStorageResponseBody()));

        performDefaultGet(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.ID_PATH,
                          requestBuilderCustomizer, "could not retrieve the prioritized data storage", created.getId());
    }

    @Test
    public void testRetrievePrioritizedDataStorages() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                .addParameter("type", created.getDataStorageType().toString());
        customizer.documentRequestParameters(RequestDocumentation.parameterWithName("type")
                .description("Prioritized data storage type")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(String.class.getSimpleName()),
                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Available values: " + Arrays.stream(DataStorageType.values())
                                            .map(Enum::name).reduce((first, second) -> first + ", " + second).get())));
        performDefaultGet(PrioritizedDataStorageController.BASE_PATH, customizer,
                          "could not retrieve the prioritized data storage");
    }

    @Test
    public void testRetrieveByType() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addParameter("type", DataStorageType.ONLINE.name());
        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(RequestDocumentation
                .parameterWithName("type").description("the wanted Data Storage Type (ONLINE, NEARLINE)")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(String.class.getSimpleName()),
                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Available values: ONLINE, NEARLINE"))));
        performDefaultGet(PrioritizedDataStorageController.BASE_PATH, requestBuilderCustomizer,
                          "could not retrieve the prioritized data storage");
    }

    @Test
    public void testUpdate() throws ModuleException, URISyntaxException, IOException {
        PrioritizedDataStorage created = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        created.getDataStorageConfiguration().setIsActive(false);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.dataStorageConfiguration.active",
                                                                       Matchers.is(Boolean.FALSE)));
        requestBuilderCustomizer.document(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName("id").description("the prioritized data storage id")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Number"), Attributes
                        .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Should be a whole number"))));
        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Prioritized data storage")), documentPrioritizedDataStorageRequestBody(false)));
        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedResponseFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Prioritized data storage")), documentPrioritizedDataStorageResponseBody()));
        performDefaultPut(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.ID_PATH,
                          created, requestBuilderCustomizer, "could not update the prioritized data storage",
                          created.getId());
    }

    @Test
    public void testIncreasePriority() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created1 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        PrioritizedDataStorage created2 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_2);
        Assert.assertEquals("created1 priority should be 0", 0L, created1.getPriority().longValue());
        Assert.assertEquals("created2 priority should be 1", 1L, created2.getPriority().longValue());
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.document(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName("id").description("the prioritized data storage id")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Number"), Attributes
                        .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Should be a whole number"))));
        performDefaultPut(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.UP_PATH, "",
                          requestBuilderCustomizer, "could not increase the priority of created2", created2.getId());
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        created1 = prioritizedDataStorageRepository.findById(created1.getId()).get();
        created2 = prioritizedDataStorageRepository.findById(created2.getId()).get();
        Assert.assertEquals("created2 should now has a priority of 0", 0L, created2.getPriority().longValue());
        Assert.assertEquals("created1 should now has a priority of 1", 1L, created1.getPriority().longValue());
    }

    @Test
    public void testDecreasePriority() throws ModuleException, IOException, URISyntaxException {
        PrioritizedDataStorage created1 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_1);
        PrioritizedDataStorage created2 = createPrioritizedDataStorage(DATA_STORAGE_CONF_LABEL_2);
        Assert.assertEquals("created1 priority should be 0", 0L, created1.getPriority().longValue());
        Assert.assertEquals("created2 priority should be 1", 1L, created2.getPriority().longValue());
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.document(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName("id").description("the prioritized data storage id")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("Number"), Attributes
                        .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Should be a whole number"))));
        performDefaultPut(PrioritizedDataStorageController.BASE_PATH + PrioritizedDataStorageController.DOWN_PATH, "",
                          requestBuilderCustomizer, "could not decrease the priority of created1", created1.getId());
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        created1 = prioritizedDataStorageRepository.findById(created1.getId()).get();
        created2 = prioritizedDataStorageRepository.findById(created2.getId()).get();
        Assert.assertEquals("created2 should now has a priority of 0", 0L, created2.getPriority().longValue());
        Assert.assertEquals("created1 should now has a priority of 1", 1L, created1.getPriority().longValue());
    }

    @After
    public void cleanUp() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        subscriber.purgeQueue(DataStorageEvent.class, DataStorageEventHandler.class);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        aipDao.deleteAll();
        prioritizedDataStorageRepository.deleteAll();
        pluginRepo.deleteAll();
    }

    private PrioritizedDataStorage createPrioritizedDataStorage(String label)
            throws IOException, URISyntaxException, ModuleException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        URL baseStorageLocation = new URL("file", "", Paths.get("target/AIPControllerIT").toFile().getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L),
                     IPluginParam.build(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()));
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        return prioritizedDataStorageService.create(dataStorageConf);
    }

    private List<FieldDescriptor> documentPrioritizedDataStorageRequestBody(boolean creation) {
        ConstrainedFields constrainedFields = new ConstrainedFields(PrioritizedDataStorage.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        if (!creation) {
            fields.add(constrainedFields.withPath("id", "id", "PrioritizedDataStorage identifier",
                                                  "Should be a whole number"));
            fields.add(constrainedFields.withPath("priority", "priority",
                                                  "PrioritizedDataStorage priority. 0 being the highest priority"));
        }
        fields.add(constrainedFields.withPath("dataStorageType", "dataStorageType", "PrioritizedDataStorage type",
                                              "Available values: ONLINE, NEARLINE"));
        fields.add(constrainedFields
                .withPath("dataStorageConfiguration", "dataStorageConfiguration", "DataStorage configuration",
                          "Should respect " + PluginConfiguration.class.getSimpleName() + " structure"));

        return fields;
    }

    private List<FieldDescriptor> documentPrioritizedDataStorageResponseBody() {
        ConstrainedFields constrainedFields = new ConstrainedFields(PrioritizedDataStorage.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        fields.add(constrainedFields.withPath("content.id", "id", "PrioritizedDataStorage identifier",
                                              "Should be a whole number"));
        fields.add(constrainedFields.withPath("content.priority", "priority",
                                              "PrioritizedDataStorage priority. 0 being the highest priority"));
        fields.add(constrainedFields.withPath("content.dataStorageType", "dataStorageType",
                                              "PrioritizedDataStorage type", "Available values: ONLINE, NEARLINE"));
        fields.add(constrainedFields
                .withPath("content.dataStorageConfiguration", "dataStorageConfiguration", "DataStorage configuration",
                          "Should respect " + PluginConfiguration.class.getSimpleName() + " structure"));

        return fields;
    }

}
