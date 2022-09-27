/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class FakeStorageRestClientFactory {

    public static IStorageRestClient create(List<EntityModel<StorageLocationDTO>> mockStorageLocationContent) {
        IStorageRestClient client = Mockito.mock(IStorageRestClient.class);
        Mockito.when(client.retrieve()).thenReturn(new ResponseEntity<>(mockStorageLocationContent, HttpStatus.OK));
        return client;
    }

    public static List<EntityModel<StorageLocationDTO>> createResponse(Set<StorageMetadata> storageMetadata,
                                                                       boolean isOffline) {
        return storageMetadata.stream()
                              .map(storageMeta -> new StorageLocationDTO(storageMeta.getPluginBusinessId(),
                                                                         0L,
                                                                         0L,
                                                                         0L,
                                                                         0L,
                                                                         0L,
                                                                         false,
                                                                         false,
                                                                         false,
                                                                         getStorageLocationConfiguration(storageMeta,
                                                                                                         isOffline),
                                                                         true))
                              .map(HateoasUtils::wrap)
                              .toList();
    }

    private static StorageLocationConfiguration getStorageLocationConfiguration(StorageMetadata storageMeta,
                                                                                boolean isOffline) {
        StorageLocationConfiguration configuration = new StorageLocationConfiguration(storageMeta.getPluginBusinessId(),
                                                                                      new PluginConfiguration(),
                                                                                      0L);
        configuration.setStorageType(isOffline ? StorageType.OFFLINE : StorageType.ONLINE);
        return configuration;
    }
}