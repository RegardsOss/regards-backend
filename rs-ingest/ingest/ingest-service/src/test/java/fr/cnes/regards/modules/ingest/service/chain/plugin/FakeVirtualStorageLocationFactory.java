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

import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationDto;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import org.springframework.hateoas.EntityModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

/**
 * @author Léo Mieulet
 */
public class FakeVirtualStorageLocationFactory {

    public static VirtualStorageLocation create(String virtualStorageLocationLabel,
                                                List<StorageMetadata> realStorageLocations,
                                                List<EntityModel<StorageLocationDto>> mockStorageLocationContent)
        throws PluginInitException {
        VirtualStorageLocation virtualStorageLocation = new VirtualStorageLocation();
        ReflectionTestUtils.setField(virtualStorageLocation, "virtualStorageName", virtualStorageLocationLabel);
        ReflectionTestUtils.setField(virtualStorageLocation, "realStorageLocations", realStorageLocations);
        ReflectionTestUtils.setField(virtualStorageLocation,
                                     "storageRestClient",
                                     FakeStorageRestClientFactory.create(mockStorageLocationContent));

        virtualStorageLocation.init();
        return virtualStorageLocation;
    }

    public VirtualStorageLocation createValid() throws PluginInitException {
        List<EntityModel<StorageLocationDto>> mockStorageLocationContent = FakeStorageRestClientFactory.createResponse(
            StorageLocationMock.validRealStorageLocationsWithAllDataType(),
            false);

        return FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                        StorageLocationMock.validRealStorageLocations(),
                                                        mockStorageLocationContent);
    }

    public VirtualStorageLocation createValidWithAllDataTypes() throws PluginInitException {
        List<EntityModel<StorageLocationDto>> mockStorageLocationContent = FakeStorageRestClientFactory.createResponse(
            StorageLocationMock.validRealStorageLocationsWithAllDataType(),
            false);

        return FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                        StorageLocationMock.validRealStorageLocationsWithAllDataType(),
                                                        mockStorageLocationContent);
    }

}
