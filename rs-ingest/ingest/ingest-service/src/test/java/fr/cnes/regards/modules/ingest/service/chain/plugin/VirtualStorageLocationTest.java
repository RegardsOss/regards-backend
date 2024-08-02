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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Léo Mieulet
 */
public class VirtualStorageLocationTest {

    @Test
    public void retrieve_plugin_parameter_real_storage_locations_when_param_only_contains_virtual_storage_name()
        throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(StorageLocationMock.validRequestStorageLocations_OnlyVirtual());

        // then
        assertEquals(StorageLocationMock.validRealStorageLocations(), resultingStorageMetadata);
    }

    @Test
    public void retrieve_empty() throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValidWithAllDataTypes();

        // when
        List<StorageMetadata> emptyListStorageLocations = new ArrayList<>();
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(
            emptyListStorageLocations);

        // then
        assertEquals(emptyListStorageLocations, resultingStorageMetadata);
    }

    @Test
    public void retrieve_identical_when_no_virtual_storage_location_inside_request()
        throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        List<StorageMetadata> onlyRealStorageLocations = StorageLocationMock.validRequestStorageLocations_OnlyReal();
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(
            onlyRealStorageLocations);

        // then
        assertEquals(onlyRealStorageLocations, resultingStorageMetadata);
    }

    @Test
    public void retrieve_with_override_store_path_when_provided() throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();
        String overrideStorePath = "/some/override/store/path";

        // when
        List<StorageMetadata> onlyVirtualStorage = StorageLocationMock.validRequestStorageLocations_OnlyVirtual();
        onlyVirtualStorage.forEach(m -> m.setStorePath(overrideStorePath));
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(onlyVirtualStorage);

        // then
        resultingStorageMetadata.forEach(m -> assertEquals(m.getStorePath(), overrideStorePath));

    }

    @Test
    public void retrieve_without_override_store_path_when_not_provided() throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        List<StorageMetadata> onlyVirtualStorage = StorageLocationMock.validRequestStorageLocations_OnlyVirtual();
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(onlyVirtualStorage);

        // then
        resultingStorageMetadata.forEach(m -> {
            StorageMetadata realWithStorePath = StorageLocationMock.validateRealStorageLocationWithStorePath();
            StorageMetadata realWithoutStorePath = StorageLocationMock.validateRealStorageLocationWithoutStorePath();
            if (m.getPluginBusinessId().equals(realWithStorePath.getPluginBusinessId())) {
                assertEquals(m.getStorePath(), realWithStorePath.getStorePath());
            } else {
                assertEquals(m.getStorePath(), realWithoutStorePath.getStorePath());
            }
        });

    }

    @Test
    public void retrieve_mixed_result_when_param_contains_virtual_storage_name_and_others_storage_locations()
        throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(StorageLocationMock.validRequestStorageLocations_MixedVirtualAndReal());

        // then
        List<StorageMetadata> expectedResult = Lists.newArrayList(StorageMetadata.build(StorageLocationMock.SOME_REAL_STORAGE_LOCATION_3));
        expectedResult.addAll(StorageLocationMock.validRealStorageLocations());
        assertTrue(expectedResult.size() == resultingStorageMetadata.size() && expectedResult.containsAll(
            resultingStorageMetadata));
    }

    @Test
    public void retrieve_mixed_result_with_request_store_metadata_over_configured_one()
        throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        List<StorageMetadata> resultingStorageMetadata = virtualStorageLocation.getStorageMetadata(StorageLocationMock.validRequestStorageLocations_MixedVirtualAndRealWithStoragePath());

        // then
        List<StorageMetadata> expectedResult = Lists.newArrayList(StorageMetadata.build(StorageLocationMock.SOME_REAL_STORAGE_LOCATION_3));
        // result contains the list of storage locations provided as plugin param, but every location path are from the request
        expectedResult.addAll(StorageLocationMock.storageLocationsOverrideByStoragePath(StorageLocationMock.validRealStorageLocations(),
                                                                                        StorageLocationMock.A_REQUEST_STORAGE_PATH));
        assertTrue(expectedResult.size() == resultingStorageMetadata.size() && expectedResult.containsAll(
            resultingStorageMetadata));
    }

    @Test(expected = ModuleException.class)
    public void error_when_providing_virtual_storage_with_attributes_defined()
        throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        // raise exception as conf not readable
        virtualStorageLocation.getStorageMetadata(StorageLocationMock.invalidRequestStorageLocations_VirtualWithUnreadableConf());
    }

    @Test(expected = ModuleException.class)
    public void error_when_storage_location_is_both_on_real_storage_location_and_inside_request()
        throws ModuleException, PluginInitException {
        // given
        VirtualStorageLocation virtualStorageLocation = new FakeVirtualStorageLocationFactory().createValid();

        // when
        // raise exception as conf not readable
        virtualStorageLocation.getStorageMetadata(StorageLocationMock.invalidRequestStorageLocations_DefineSameStorageLocationThanPluginParam());
    }

    @Test(expected = PluginInitException.class)
    public void error_plugin_init_real_location_empty() throws PluginInitException {
        FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                 Collections.emptyList(),
                                                 Collections.emptyList());
    }

    @Test(expected = PluginInitException.class)
    public void error_plugin_init_real_location_missing_datatypes() throws PluginInitException {
        FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                 StorageLocationMock.invalidRealStorageLocations(),
                                                 Collections.emptyList());
    }

    @Test(expected = PluginInitException.class)
    public void error_plugin_storage_response_real_storage_location_not_found() throws PluginInitException {
        List<StorageMetadata> storageMetadata = StorageLocationMock.validRealStorageLocations();
        List<StorageMetadata> partialStorageMetadata = List.of(storageMetadata.stream().findAny().get());
        FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                 storageMetadata,
                                                 FakeStorageRestClientFactory.createResponse(partialStorageMetadata,
                                                                                             false));
    }

    @Test(expected = PluginInitException.class)
    public void error_plugin_storage_response_real_storage_offline() throws PluginInitException {
        FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                 StorageLocationMock.validRealStorageLocations(),
                                                 FakeStorageRestClientFactory.createResponse(StorageLocationMock.validRealStorageLocations(),
                                                                                             true));
    }

    @Test(expected = PluginInitException.class)
    public void error_plugin_storage_response_virtual_storage_exist() throws PluginInitException {
        List<StorageMetadata> storageMetadataOnStorage = StorageLocationMock.validRealStorageLocations();
        storageMetadataOnStorage.add(StorageMetadata.build(StorageLocationMock.A_VIRTUAL_STORAGE_NAME));
        FakeVirtualStorageLocationFactory.create(StorageLocationMock.A_VIRTUAL_STORAGE_NAME,
                                                 StorageLocationMock.validRealStorageLocations(),
                                                 FakeStorageRestClientFactory.createResponse(storageMetadataOnStorage,
                                                                                             false));
    }
}
