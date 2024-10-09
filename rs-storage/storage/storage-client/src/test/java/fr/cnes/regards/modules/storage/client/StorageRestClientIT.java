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
package fr.cnes.regards.modules.storage.client;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import feign.Response;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.plugin.SimpleOnlineTestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test class for REST Client
 *
 * @author Sébastien Binda
 */
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_tests",
                                   "regards.amqp.enabled=true",
                                   "regards.storage.cache.path:target/cache" })
public class StorageRestClientIT extends AbstractRegardsWebIT {

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    private IStorageRestClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    protected IFileReferenceRepository fileRepo;

    private static final String ONLINE_CONF = "ONLINE_CONF";

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        fileRepo.deleteAll();
        client = FeignClientBuilder.build(new TokenClientProvider<>(IStorageRestClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager),
                                          gson,
                                          requestTemplate -> requestTemplate.header("Content-Type",
                                                                                    MediaType.APPLICATION_JSON_VALUE));
        if (storageLocationConfService.search(ONLINE_CONF).isEmpty()) {
            initDataStoragePluginConfiguration();
        }
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    @Test
    public void donwload() {
        Response response = client.downloadFile("huhuhuhu", false);
        Assert.assertEquals(HttpStatus.NOT_FOUND.value(), response.status());
    }

    @Test
    public void export() throws IOException {
        // Create an entity for test
        for (int i = 0; i < 100; i++) {
            fileRefService.create(Sets.newHashSet("someone", "someone-else"),
                                  new FileReferenceMetaInfo("123456" + i,
                                                            "MD5",
                                                            "file.test_" + i,
                                                            10L,
                                                            MediaType.APPLICATION_JSON),
                                  new FileLocation("somewhere", "file://plop/plip.file_" + i, false),
                                  false);
        }
        Response response = client.export();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.status());
        InputStream is = response.body().asInputStream();
        int i;
        char c;
        while ((i = is.read()) != -1) {
            c = (char) i;
            System.out.print(c);
        }
        Assert.assertNotNull(response);
    }

    @Test
    public void retrieveStorageLocations() {
        ResponseEntity<List<EntityModel<StorageLocationDto>>> response = client.retrieve();
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        // Expected 2 storages. One created in init method and 1 cache system
        Assert.assertEquals(2, response.getBody().size());
        Assert.assertTrue(response.getBody().stream().anyMatch(s -> s.getContent().getName().equals(ONLINE_CONF)));
        Assert.assertTrue(response.getBody()
                                  .stream()
                                  .anyMatch(s -> s.getContent().getName().equals(CacheService.CACHE_NAME)));
    }

    @Test
    public void getFileReferencesWithoutOwners() {
        String storageName = "somewhere";
        Set<String> checksums = Sets.newHashSet(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        for (String checksum : checksums) {
            fileRefService.create(Sets.newHashSet("someone", "someone-else"),
                                  new FileReferenceMetaInfo(checksum,
                                                            "MD5",
                                                            "file_ref.txt",
                                                            1L,
                                                            MediaType.APPLICATION_JSON),
                                  new FileLocation("somewhere", "file://sample/file_ref.txt", false),
                                  true);
        }
        ResponseEntity<Set<FileReferenceDto>> response = client.getFileReferencesWithoutOwners(storageName, checksums);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertEquals("Unexpected number of file references found",
                            checksums.size(),
                            Objects.requireNonNull(response.getBody()).size());
        Assert.assertEquals("Unexpected checksums retrieved",
                            checksums,
                            response.getBody()
                                    .stream()
                                    .map(f -> f.getMetaInfo().getChecksum())
                                    .collect(Collectors.toSet()));
    }

    private StorageLocationConfiguration initDataStoragePluginConfiguration() {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineTestClient.class);
            Files.createDirectories(Paths.get("target/online-storage/"));

            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleOnlineTestClient.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                               "target/online-storage/"),
                                                            IPluginParam.build(SimpleOnlineTestClient.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                               "error.*"),
                                                            IPluginParam.build(SimpleOnlineTestClient.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                               "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(ONLINE_CONF,
                                                                          parameters,
                                                                          0,
                                                                          dataStoMeta.getPluginId());
            return storageLocationConfService.create(ONLINE_CONF, dataStorageConf, 1_000_000L);
        } catch (IOException | ModuleException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

}
