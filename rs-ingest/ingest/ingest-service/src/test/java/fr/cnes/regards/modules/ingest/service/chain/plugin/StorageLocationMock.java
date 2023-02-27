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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;

import java.util.List;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class StorageLocationMock {

    protected static String SOME_REAL_STORAGE_LOCATION_1 = "SOME_REAL_STORAGE_LOCATION_1";

    protected static String SOME_REAL_STORAGE_LOCATION_2 = "SOME_REAL_STORAGE_LOCATION_2";

    protected static String SOME_REAL_STORAGE_LOCATION_3 = "SOME_REAL_STORAGE_LOCATION_3";

    protected static String A_VIRTUAL_STORAGE_NAME = "A_VIRTUAL_STORAGE_NAME";

    protected static String A_REQUEST_STORAGE_PATH = "a request storage path";

    // ------- Plugins configurations -------

    public static List<StorageMetadata> validRealStorageLocations() {
        return Lists.newArrayList(validateRealStorageLocationWithStorePath(),
                                  validateRealStorageLocationWithoutStorePath());
    }

    public static StorageMetadata validateRealStorageLocationWithStorePath() {
        return StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_1, "some/store/path", Set.of());
    }

    public static StorageMetadata validateRealStorageLocationWithoutStorePath() {
        return StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_2);
    }

    public static List<StorageMetadata> storageLocationsOverrideByStoragePath(List<StorageMetadata> storageLocations,
                                                                              String storagePath) {
        for (StorageMetadata storageLocation : storageLocations) {
            storageLocation.setStorePath(storagePath);
        }
        return storageLocations;
    }

    public static List<StorageMetadata> invalidRealStorageLocations() {
        // all values from {@link DataType} are not used, which is incorrect
        return Lists.newArrayList(StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_1,
                                                        null,
                                                        Set.of(DataType.DESCRIPTION, DataType.AIP)),
                                  StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_2,
                                                        null,
                                                        Set.of(DataType.QUICKLOOK_HD,
                                                               DataType.QUICKLOOK_MD,
                                                               DataType.QUICKLOOK_SD)));
    }

    public static List<StorageMetadata> validRealStorageLocationsWithAllDataType() {
        return Lists.newArrayList(StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_1,
                                                        null,
                                                        Set.of(DataType.DESCRIPTION,
                                                               DataType.AIP,
                                                               DataType.RAWDATA,
                                                               DataType.DOCUMENT)),
                                  StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_2,
                                                        null,
                                                        Set.of(DataType.QUICKLOOK_HD,
                                                               DataType.QUICKLOOK_MD,
                                                               DataType.QUICKLOOK_SD,
                                                               DataType.THUMBNAIL,
                                                               DataType.OTHER)));
    }
    // ------- Request payload -------

    public static List<StorageMetadata> validRequestStorageLocations_OnlyVirtual() {
        return Lists.newArrayList(StorageMetadata.build(A_VIRTUAL_STORAGE_NAME));
    }

    public static List<StorageMetadata> validRequestStorageLocations_MixedVirtualAndReal() {
        return Lists.newArrayList(StorageMetadata.build(A_VIRTUAL_STORAGE_NAME),
                                  StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_3));
    }

    public static List<StorageMetadata> validRequestStorageLocations_MixedVirtualAndRealWithStoragePath() {
        return Lists.newArrayList(StorageMetadata.build(A_VIRTUAL_STORAGE_NAME, A_REQUEST_STORAGE_PATH, Set.of()),
                                  StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_3));
    }

    public static List<StorageMetadata> validRequestStorageLocations_OnlyReal() {
        return Lists.newArrayList(StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_1),
                                  StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_2));
    }

    public static List<StorageMetadata> invalidRequestStorageLocations_VirtualWithUnreadableConf() {
        return Lists.newArrayList(StorageMetadata.build(A_VIRTUAL_STORAGE_NAME,
                                                        "some storage path",
                                                        Set.of(DataType.DESCRIPTION)));
    }

    public static List<StorageMetadata> invalidRequestStorageLocations_DefineSameStorageLocationThanPluginParam() {
        return Lists.newArrayList(StorageMetadata.build(A_VIRTUAL_STORAGE_NAME),
                                  StorageMetadata.build(SOME_REAL_STORAGE_LOCATION_2));
    }
}
