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
package fr.cnes.regards.modules.fileaccess.client;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.fileaccess.client.plugin.TestSimpleOnlineDataStorage;
import fr.cnes.regards.modules.fileaccess.dao.IStorageLocationConfigurationRepository;
import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Test class for REST Feign Client {@link IStorageLocationConfigurationClient}.
 *
 * @author Iliana Ghazali
 */
@ActiveProfiles(value = { "default", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_location_config_client_it" })
public class StorageLocationConfigurationClientIT extends AbstractRegardsWebIT {

    // CONSTANTS

    public static final String OFFLINE_STORAGE_NAME = "offline-storage";

    private static final String ONLINE_STORAGE_NAME = "online-storage";

    @Value("${server.address}")
    private String serverAddress;

    // REPOSITORIES

    @Autowired
    private IStorageLocationConfigurationRepository storageLocationConfigRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfigRepository;

    // SERVICES

    @Autowired
    private Gson gson;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    // FEIGN

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    private IStorageLocationConfigurationClient client;

    @Before
    public void init() {
        initFeignClient();
        cleanRepositories();
        initData();
    }

    @Test
    public void givenStorageConfig_whenCreateOne_thenOk() throws ModuleException {
        // GIVEN storage location configurations created in init method
        String storageName = "tested-storage";
        StorageLocationConfigurationDto storageConfigToCreate = new StorageLocationConfiguration(storageName,
                                                                                                 getOnlinePluginConfiguration(
                                                                                                     storageName),
                                                                                                 30L).toDto();
        // WHEN
        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = client.createStorageLocationConfig(
            storageConfigToCreate);
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getContent()).isNotNull();
        Assertions.assertThat(storageLocationConfigRepository.findByName(storageName))
                  .as("The storage location was not created as expected.")
                  .isPresent();
    }

    @Test
    public void givenStorageConfigs_whenRetrieveAll_thenOk() {
        // GIVEN storage location configurations created in init method
        // WHEN
        ResponseEntity<List<EntityModel<StorageLocationConfigurationDto>>> response = client.retrieveAllStorageLocationConfigs();
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).as("Unexpected number of storage location configurations").hasSize(2);
        Assertions.assertThat(response.getBody()
                                      .stream()
                                      .map(s -> Objects.requireNonNull(s.getContent()).getName())
                                      .toList())
                  .as("Unexpected storage location configuration names retrieved.")
                  .containsExactlyInAnyOrderElementsOf(List.of(ONLINE_STORAGE_NAME, OFFLINE_STORAGE_NAME));
    }

    @Test
    public void givenStorageConfigs_whenRetrieveOne_thenOk() {
        // GIVEN storage location configurations created in init method
        // WHEN
        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = client.retrieveStorageLocationConfigByName(
            ONLINE_STORAGE_NAME);
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().getContent()).isNotNull();
        Assertions.assertThat(response.getBody().getContent().getName())
                  .as("Unexpected storage location configuration retrieved.")
                  .isEqualTo(ONLINE_STORAGE_NAME);
    }

    @Test
    public void givenStorageConfigs_whenUpdateOne_thenOk() throws ModuleException {
        // GIVEN storage location configurations created in init method
        // update storage location retrieved
        long updatedAllocatedSize = 5000;
        StorageLocationConfiguration storageConfigToUpdate = storageLocationConfigRepository.findByName(
            ONLINE_STORAGE_NAME).orElseThrow();
        storageConfigToUpdate.setAllocatedSizeInKo(updatedAllocatedSize);

        // WHEN
        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = client.updateStorageLocationConfigByName(
            ONLINE_STORAGE_NAME,
            storageConfigToUpdate.toDto());
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        StorageLocationConfigurationDto updatedStorageConfig = response.getBody().getContent();
        Assertions.assertThat(updatedStorageConfig).isNotNull();
        Assertions.assertThat(updatedStorageConfig.getName())
                  .as("Unexpected storage location configuration retrieved.")
                  .isEqualTo(ONLINE_STORAGE_NAME);
        Assertions.assertThat(updatedStorageConfig.getAllocatedSizeInKo())
                  .as("The storage location was not updated as expected.")
                  .isEqualTo(updatedAllocatedSize);
    }

    @Test
    public void givenStorageConfigs_whenDeleteOne_thenOk() throws ModuleException {
        // GIVEN storage location configurations created in init method
        // WHEN
        ResponseEntity<Void> response = client.deleteStorageLocationConfigByName(ONLINE_STORAGE_NAME);
        // THEN
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(storageLocationConfigRepository.findByName(ONLINE_STORAGE_NAME))
                  .as("The storage location was not deleted as expected.")
                  .isEmpty();
    }

    //------
    // UTILS
    //------

    private void initFeignClient() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IStorageLocationConfigurationClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager),
                                          gson,
                                          requestTemplate -> requestTemplate.header("Content-Type",
                                                                                    MediaType.APPLICATION_JSON_VALUE));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    private void cleanRepositories() {
        storageLocationConfigRepository.deleteAll();
        pluginConfigRepository.deleteAll();
    }

    private void initData() {
        PluginConfiguration onlinePluginConfig = getOnlinePluginConfiguration(ONLINE_STORAGE_NAME);
        storageLocationConfigRepository.saveAll(List.of(new StorageLocationConfiguration(ONLINE_STORAGE_NAME,
                                                                                         onlinePluginConfig,
                                                                                         1_000_000L),
                                                        new StorageLocationConfiguration(OFFLINE_STORAGE_NAME,
                                                                                         null,
                                                                                         null)));
    }

    private PluginConfiguration getOnlinePluginConfiguration(String name) {
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(TestSimpleOnlineDataStorage.class);
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(TestSimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                           "target/storage/ONLINE-STORAGE"),
                                                        IPluginParam.build(TestSimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                           "error.*"),
                                                        IPluginParam.build(TestSimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                           "delErr.*"));
        return pluginConfigRepository.save(new PluginConfiguration(pluginMetadata.getPluginId(),
                                                                   name,
                                                                   UUID.randomUUID().toString(),
                                                                   "1.0",
                                                                   0,
                                                                   true,
                                                                   null,
                                                                   parameters,
                                                                   pluginMetadata));
    }

}
