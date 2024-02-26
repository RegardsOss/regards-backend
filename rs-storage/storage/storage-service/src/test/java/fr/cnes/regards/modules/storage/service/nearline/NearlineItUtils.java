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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.storage.service.nearline;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IStorageLocationConfigurationRepostory;
import fr.cnes.regards.modules.storage.domain.database.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author tguillou
 */
@Service
public class NearlineItUtils {

    @Autowired
    public IFileReferenceRepository fileReferenceRepository;

    @Autowired
    protected ICacheFileRepository cacheFileRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IStorageLocationConfigurationRepostory storageLocationConfigurationRepository;

    private StorageNearlineMocked pluginStorageNearlineMockedInstance;

    public static final String FILE_1 = "FILE_CACHED_1";

    public static final String FILE_2 = "FILE_CACHED_2";

    public static final String FILE_T3 = "FILE_T3";

    public static final String FILE_NEARLINE = "FILE_NEARLINE";

    public static final OffsetDateTime EXPIRED_DATE = OffsetDateTime.now().minusDays(5);

    public StorageNearlineMocked getPluginStorageNearlineMockedInstance() {
        return pluginStorageNearlineMockedInstance;
    }

    public void initOnlineNearlineAndOfflineStoragesAndPlugins(PluginService mockedPluginService)
        throws ModuleException, NotAvailablePluginConfigurationException {
        storageLocationConfigurationRepository.deleteAll();
        PluginConfiguration pluginConfiguration = buildNearlineConfiguration();
        pluginConfigurationRepository.save(pluginConfiguration);
        List<StorageLocationConfiguration> storages = List.of(buildStorage("Online"),
                                                              buildStorage("Nearline", pluginConfiguration),
                                                              buildStorage("Offline"));
        storageLocationConfigurationRepository.saveAll(storages);
        pluginStorageNearlineMockedInstance = Mockito.mock(StorageNearlineMocked.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mockedPluginService.getPlugin(Mockito.anyString()))
               .thenReturn(pluginStorageNearlineMockedInstance);
    }

    public void simulateFilePassT2toT3(FileReference fileNearline) {
        fileNearline.getMetaInfo().setFileName(FILE_NEARLINE + StorageNearlineMocked.T3_PATTERN);
        fileReferenceRepository.save(fileNearline);
    }

    public void storeT3Files(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files)
                                              .map(file -> buildFileReference(file, "Nearline"))
                                              .toList());
    }

    public void storeT2Files(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files)
                                              .map(file -> buildFileReference(file, "Nearline"))
                                              .toList());
    }

    public void storeCacheFiles(String... files) {
        cacheFileRepository.saveAll(Arrays.stream(files).map(this::buildCachefile).toList());
    }

    public void storeOfflineFiles(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files).map(file -> buildFileReference(file, "Offline")).toList());
    }

    public void storeOnlineFiles(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files).map(file -> buildFileReference(file, "Online")).toList());
    }

    public PluginConfiguration buildNearlineConfiguration() {
        //        PluginMetaData metaData = PluginUtils.createPluginMetaData(StorageNearlineMocked.class);
        PluginConfiguration configuration = new PluginConfiguration("Nearline",
                                                                    Set.of(),
                                                                    0,
                                                                    StorageNearlineMocked.PLUGIN_ID);
        configuration.setMetaData(new PluginMetaData());
        configuration.setVersion("1.0");
        configuration.setIsActive(true);
        return configuration;
    }

    public CacheFile buildCachefile(String name) {
        return CacheFile.buildFileInternalCache(name,
                                                10L,
                                                name,
                                                MimeType.valueOf("application/json"),
                                                null,
                                                OffsetDateTime.now().plusHours(1),
                                                Set.of("groupId"),
                                                "type");
    }

    public CacheFile buildExternalCachefile(String name) {
        String storageName = "Nearline";
        return CacheFile.buildFileExternalCache(name,
                                                10L,
                                                name,
                                                MimeType.valueOf("application/json"),
                                                null,
                                                OffsetDateTime.now().plusHours(1),
                                                Set.of("groupId"),
                                                "type",
                                                storageName);
    }

    public StorageLocationConfiguration buildStorage(String name) {
        return buildStorage(name, null);
    }

    public StorageLocationConfiguration buildStorage(String name, PluginConfiguration pluginConfiguration) {
        StorageLocationConfiguration storageLocationConfiguration = new StorageLocationConfiguration(name,
                                                                                                     pluginConfiguration,
                                                                                                     50L);
        storageLocationConfiguration.setStorageType(StorageType.valueOf(name.toUpperCase()));
        return storageLocationConfiguration;
    }

    public FileReference buildFileReference(String name, String storage) {
        FileReference fileReference = new FileReference();
        fileReference.setMetaInfo(new FileReferenceMetaInfo(name,
                                                            "algorithm",
                                                            name,
                                                            50L,
                                                            MimeType.valueOf("application/json")));
        fileReference.setLocation(new FileLocation(storage, storage, false));
        return fileReference;
    }
}
