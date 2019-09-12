/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.rest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;
import fr.cnes.regards.modules.storagelight.rest.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.location.PrioritizedStorageService;
import fr.cnes.regards.modules.storagelight.service.location.StoragePluginConfigurationHandler;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_rest_it",
        "regards.storage.cache.path=target/cache" })
public class FileReferenceControllerIT extends AbstractRegardsTransactionalIT {

    private static final String TARGET_STORAGE = "target";

    private static final String STORAGE_PATH = "target/ONLINE-STORAGE";

    @Autowired
    private FileStorageRequestService storeReqService;

    @Autowired
    private PrioritizedStorageService prioritizedDataStorageService;

    @Autowired
    protected StoragePluginConfigurationHandler storagePlgConfHandler;

    @Autowired
    protected IFileReferenceRepository fileRepo;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private String storedFileChecksum;

    private void clear() throws IOException {
        fileRepo.deleteAll();
        prioritizedDataStorageService.search(StorageType.ONLINE).forEach(c -> {
            try {
                prioritizedDataStorageService.delete(c.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        if (Files.exists(Paths.get("target/storage"))) {
            FileUtils.deleteDirectory(Paths.get(STORAGE_PATH).toFile());
        }
    }

    @Before
    public void init()
            throws NoSuchAlgorithmException, FileNotFoundException, IOException, InterruptedException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        clear();
        initDataStoragePluginConfiguration();
        // Store a file for tests
        Path filePath = Paths.get("src/test/resources/test-file.txt");
        String algorithm = "md5";
        String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(filePath.toFile()), algorithm);
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum, algorithm,
                filePath.getFileName().toString(), null, MediaType.APPLICATION_OCTET_STREAM);
        storeReqService.handleRequest("rest-test", metaInfo, filePath.toAbsolutePath().toUri().toURL().toString(), TARGET_STORAGE,
                                      Optional.of("/sub/dir/1/"), UUID.randomUUID().toString());
        Thread.sleep(5_000);
        storedFileChecksum = checksum;
    }

    @Test
    public void downloadFileError() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNotFound();
        performDefaultGet(FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH,
                          requestBuilderCustomizer, "File download response status should be NOT_FOUND.",
                          UUID.randomUUID().toString());
    }

    @Test
    public void downloadFileSuccess() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(FileReferenceController.FILE_PATH + FileReferenceController.DOWNLOAD_PATH,
                          requestBuilderCustomizer, "File download response status should be OK", storedFileChecksum);
    }

    private void initDataStoragePluginConfiguration() throws ModuleException {
        try {
            PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
            Files.createDirectories(Paths.get(STORAGE_PATH));

            Set<IPluginParam> parameters = IPluginParam
                    .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                            STORAGE_PATH),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                         IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
            PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, TARGET_STORAGE, parameters, 0);
            dataStorageConf.setIsActive(true);
            prioritizedDataStorageService.create(dataStorageConf);
            // storagePlgConfHandler.refresh();
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

}
