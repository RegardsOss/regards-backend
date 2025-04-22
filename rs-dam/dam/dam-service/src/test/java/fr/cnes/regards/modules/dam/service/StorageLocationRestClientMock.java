/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.storage.client.IStorageLocationRestClient;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author Sébastien Binda
 **/
@Primary
@Component
public class StorageLocationRestClientMock implements IStorageLocationRestClient {

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve() {
        PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setInterfaceNames(Sets.newHashSet(StorageLocationDto.class.getName()));
        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setMetaData(pluginMetaData);
        StorageLocationConfigurationDto configuration = new StorageLocationConfigurationDto("name",
                                                                                            pluginConfiguration.toDto(),
                                                                                            StorageType.ONLINE,
                                                                                            0L);
        StorageLocationDto storageLocationDTO = StorageLocationDto.build("Local", configuration)
                                                                  .withAllowPhysicalDeletion()
                                                                  .withRunningProcessesInformation(true,
                                                                                                   true,
                                                                                                   true,
                                                                                                   false);
        return ResponseEntity.ok(Collections.singletonList(EntityModel.of(storageLocationDTO)));
    }
}
