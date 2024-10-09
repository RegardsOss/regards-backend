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
package fr.cnes.regards.modules.filecatalog.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.fileaccess.client.IStorageLocationConfigurationClient;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock for feign client {@link IStorageLocationConfigurationClient}.
 *
 * @author Iliana Ghazali
 */
@Service
public class StorageLocationConfigurationMock implements IStorageLocationConfigurationClient {

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationConfigurationDto>>> retrieveAllStorageLocationConfigs() {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<StorageLocationConfigurationDto>> retrieveStorageLocationConfigByName(String storageName) {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<StorageLocationConfigurationDto>> createStorageLocationConfig(
        StorageLocationConfigurationDto storageLocationConfigDto) throws ModuleException {
        return null;
    }

    @Override
    public ResponseEntity<EntityModel<StorageLocationConfigurationDto>> updateStorageLocationConfigByName(String storageName,
                                                                                                          StorageLocationConfigurationDto storageLocationConfigDto)
        throws ModuleException {
        return null;
    }

    @Override
    public ResponseEntity<Void> deleteStorageLocationConfigByName(String storageName) throws ModuleException {
        return null;
    }
}
