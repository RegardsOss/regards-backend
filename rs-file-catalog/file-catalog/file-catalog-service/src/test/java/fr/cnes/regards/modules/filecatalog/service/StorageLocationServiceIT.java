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

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.domain.FileLocation;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.domain.StorageLocation;
import fr.cnes.regards.modules.filecatalog.domain.request.FileDeletionRequest;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Test for {@link fr.cnes.regards.modules.filecatalog.service.location.StorageLocationService}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "noscheduler", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_location_tests" },
                    locations = { "classpath:application-test.properties" })
public class StorageLocationServiceIT extends AbstractFileCatalogIT {

    @Before
    public void initialize() throws ModuleException {
        super.init();
    }

    @Test
    public void retrieve_one_location_no_monitoring() throws ModuleException {
        String storage = "STAF";

        // Given
        storageLocationRepo.save(new StorageLocation(storage));

        // Mock
        Mockito.when(storageLocationConfigurationClient.retrieveStorageLocationConfigByName(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(HateoasUtils.wrap(new StorageLocationConfigurationDto(1L,
                                                                                                      storage,
                                                                                                      null,
                                                                                                      StorageType.OFFLINE,
                                                                                                      0L,
                                                                                                      0L)),
                                                HttpStatusCode.valueOf(200)));

        // When
        StorageLocationDto loc = storageLocationService.findStorageLocationByName(storage);

        // Then
        Assertions.assertNotNull(loc, "The storage should have been found");
        Assertions.assertEquals(storage, loc.getName(), "The storage should be the one saved earlier");
        Assertions.assertEquals(storage,
                                loc.getConfiguration().getName(),
                                "The configuration should be the mocked one");
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1))
               .retrieveStorageLocationConfigByName(Mockito.any());

        // When
        loc = storageLocationService.findStorageLocationByName(storage);

        // Then
        Assertions.assertNotNull(loc, "The storage should have been found");
        Assertions.assertEquals(storage, loc.getName(), "The storage should be the one saved earlier");
        Assertions.assertEquals(storage,
                                loc.getConfiguration().getName(),
                                "The configuration should be the mocked one");
        // Verify that the client was not called again (because the cache was used)
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1))
               .retrieveStorageLocationConfigByName(Mockito.any());

        // When
        storageLocationService.invalidateCache(storage);
        loc = storageLocationService.findStorageLocationByName(storage);

        // Then
        Assertions.assertNotNull(loc, "The storage should have been found");
        Assertions.assertEquals(storage, loc.getName(), "The storage should be the one saved earlier");
        Assertions.assertEquals(storage,
                                loc.getConfiguration().getName(),
                                "The configuration should be the mocked one");
        // Verify that the client was called again (because the cache was cleaned)
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(2))
               .retrieveStorageLocationConfigByName(Mockito.any());
    }

    @Test
    public void retrieve_all_locations_no_monitoring() throws ModuleException {
        String storage1 = "STAF";
        String storage2 = "Datalake";
        String storage3 = "Minio";
        List<String> storages = List.of(storage1, storage2, storage3);

        // Given
        storageLocationRepo.save(new StorageLocation(storage1));
        storageLocationRepo.save(new StorageLocation(storage2));
        storageLocationRepo.save(new StorageLocation(storage3));

        // Mock
        StorageLocationConfigurationDto config1 = new StorageLocationConfigurationDto(1L,
                                                                                      storage1,
                                                                                      null,
                                                                                      StorageType.OFFLINE,
                                                                                      0L,
                                                                                      0L);
        StorageLocationConfigurationDto config2 = new StorageLocationConfigurationDto(2L,
                                                                                      storage2,
                                                                                      null,
                                                                                      StorageType.NEARLINE,
                                                                                      0L,
                                                                                      0L);
        StorageLocationConfigurationDto config3 = new StorageLocationConfigurationDto(3L,
                                                                                      storage3,
                                                                                      null,
                                                                                      StorageType.ONLINE,
                                                                                      0L,
                                                                                      0L);
        List<StorageLocationConfigurationDto> configs = List.of(config1, config2, config3);

        ResponseEntity<List<EntityModel<StorageLocationConfigurationDto>>> response = new ResponseEntity<>(HateoasUtils.wrapList(
            List.of(config1, config2, config3)), HttpStatusCode.valueOf(200));
        Mockito.when(storageLocationConfigurationClient.retrieveAllStorageLocationConfigs()).thenReturn(response);

        // When
        List<StorageLocationDto> locs = storageLocationService.findAllStorageLocations();

        // Then
        Assertions.assertEquals(storages.size(), locs.size(), "There should be one storage found for each one saved");
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1)).retrieveAllStorageLocationConfigs();

        for (int i = 0; i < locs.size(); i++) {
            String storage = storages.get(i);
            StorageLocationConfigurationDto config = configs.get(i);
            Optional<StorageLocationDto> foundLoc = locs.stream()
                                                        .filter(loc -> loc.getName().equals(storage))
                                                        .findFirst();
            Assertions.assertTrue(foundLoc.isPresent(), "The storage should be present");
            StorageLocationDto loc = foundLoc.get();

            Assertions.assertEquals(storage, loc.getName(), "The storage should be the one saved earlier");
            Assertions.assertEquals(config.getName(),
                                    loc.getConfiguration().getName(),
                                    "The configuration should be the mocked one");
        }

        // When
        locs = storageLocationService.findAllStorageLocations();

        // Then
        Assertions.assertEquals(storages.size(), locs.size(), "There should be one storage found for each one saved");
        // Verify that the client was not called again (because the cache was used)
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1)).retrieveAllStorageLocationConfigs();

        // When
        storageLocationService.invalidateCache(storage1);
        locs = storageLocationService.findAllStorageLocations();
        // Verify that the client was called again (because one of the storage is not in the cache anymore)
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(2)).retrieveAllStorageLocationConfigs();

    }

    @Test
    public void create_location() throws ModuleException {
        String storage = "STAF";
        StorageLocationConfigurationDto configurationDto = new StorageLocationConfigurationDto(1L,
                                                                                               storage,
                                                                                               null,
                                                                                               StorageType.OFFLINE,
                                                                                               0L,
                                                                                               0L);

        // Mock
        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = new ResponseEntity<>(HateoasUtils.wrap(
            configurationDto), HttpStatusCode.valueOf(200));
        Mockito.when(storageLocationConfigurationClient.createStorageLocationConfig(Mockito.any()))
               .thenReturn(response);

        // When
        storageLocationService.createStorageLocation(StorageLocationDto.build(storage, configurationDto));

        // Then
        Optional<StorageLocation> foundStorage = storageLocationRepo.findByName(storage);
        Assertions.assertTrue(foundStorage.isPresent(), "The storage should exist");

        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1)).createStorageLocationConfig(Mockito.any());

    }

    @Test
    public void create_location_already_exists() throws ModuleException {
        String storage = "STAF";

        //Given
        storageLocationRepo.save(new StorageLocation(storage));

        // When Then
        ModuleException exception = Assertions.assertThrows(ModuleException.class,
                                                            () -> storageLocationService.createStorageLocation(
                                                                StorageLocationDto.build(storage,
                                                                                         new StorageLocationConfigurationDto(
                                                                                             1L,
                                                                                             storage,
                                                                                             null,
                                                                                             StorageType.OFFLINE,
                                                                                             0L,
                                                                                             0L))));

        Assertions.assertTrue(exception.getMessage().contains("because it already exists"));
    }

    @Test
    public void update_location() throws ModuleException {
        String storage = "STAF";
        StorageLocationConfigurationDto configurationDto = new StorageLocationConfigurationDto(1L,
                                                                                               storage,
                                                                                               null,
                                                                                               StorageType.OFFLINE,
                                                                                               0L,
                                                                                               0L);

        // Mock
        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = new ResponseEntity<>(HateoasUtils.wrap(
            configurationDto), HttpStatusCode.valueOf(200));
        Mockito.when(storageLocationConfigurationClient.createStorageLocationConfig(Mockito.any()))
               .thenReturn(response);

        // Given
        storageLocationService.createStorageLocation(StorageLocationDto.build(storage, configurationDto));

        // new Mock
        configurationDto.setAllocatedSizeInKo(100L);
        response = new ResponseEntity<>(HateoasUtils.wrap(configurationDto), HttpStatusCode.valueOf(200));
        Mockito.when(storageLocationConfigurationClient.updateStorageLocationConfigByName(Mockito.any(), Mockito.any()))
               .thenReturn(response);

        // When
        StorageLocationDto updatedLocation = storageLocationService.updateLocationConfiguration(storage,
                                                                                                StorageLocationDto.build(
                                                                                                    storage,
                                                                                                    configurationDto));

        // Then
        Assertions.assertEquals(storage, updatedLocation.getName());
        Assertions.assertEquals(100L, updatedLocation.getConfiguration().getAllocatedSizeInKo());
        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1))
               .updateStorageLocationConfigByName(Mockito.any(), Mockito.any());

    }

    @Test
    public void delete_location() throws ModuleException {
        String storage = "STAF";
        // Given
        storageLocationRepo.save(new StorageLocation(storage));
        int requestNumber = 5;
        createFileStorageRequests(requestNumber, storage);
        FileReference fileReference = createFileReference(storage);
        createFileDeletionRequests(fileReference);
        Assertions.assertEquals(requestNumber,
                                fileStorageRequestAggregationRepository.countByStorageAndStatus(storage,
                                                                                                StorageRequestStatus.TO_HANDLE));
        Assertions.assertEquals(1,
                                fileDeletionRequestRepository.countByStorageAndStatus(storage,
                                                                                      FileRequestStatus.TO_DO));

        // Mock
        Mockito.when(storageLocationConfigurationClient.deleteStorageLocationConfigByName(Mockito.any()))
               .thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));

        // When
        storageLocationService.delete(storage);

        // Then
        Optional<StorageLocation> foundStorage = storageLocationRepo.findByName(storage);
        Assertions.assertTrue(foundStorage.isEmpty(), "The storage should be deleted");

        Mockito.verify(storageLocationConfigurationClient, Mockito.times(1))
               .deleteStorageLocationConfigByName(Mockito.any());

        Assertions.assertEquals(0,
                                fileStorageRequestAggregationRepository.countByStorageAndStatus(storage,
                                                                                                StorageRequestStatus.TO_HANDLE),
                                "There should be no more storage request");
        Assertions.assertEquals(0,
                                fileDeletionRequestRepository.countByStorageAndStatus(storage, FileRequestStatus.TO_DO),
                                "There should be no more deletion request");

        // FIXME TODO LOT 4 check file reference deletion

    }

    private FileDeletionRequest createFileDeletionRequests(FileReference fileReference) {
        FileDeletionRequest request = new FileDeletionRequest(fileReference,
                                                              true,
                                                              "groupId",
                                                              "sessionOwner",
                                                              "session");
        request.setStatus(FileRequestStatus.TO_DO);
        return this.fileDeletionRequestRepository.save(request);
    }

    /**
     * Method to create FileStorageRequests for test
     */
    private List<FileStorageRequestAggregation> createFileStorageRequests(int nbRequests, String storage) {
        List<FileStorageRequestAggregation> createdStorageRequests = new ArrayList<>();
        // init parameters
        String owner = "test";
        String algorithm = "dynamic";
        String fileName = "randomFile.test";
        Long fileSize = 20L;
        MimeType mimeType = MediaType.IMAGE_PNG;
        String originUrl = "file://" + Paths.get("src/test/resources/input/cnes.png").toAbsolutePath();
        // create requests
        for (int i = 0; i < nbRequests; i++) {
            FileReferenceMetaInfo metaInfos = new FileReferenceMetaInfo(RandomChecksumUtils.generateRandomChecksum(),
                                                                        algorithm,
                                                                        fileName + 1,
                                                                        fileSize,
                                                                        mimeType);
            FileStorageRequestAggregation request = new FileStorageRequestAggregation(owner,
                                                                                      metaInfos,
                                                                                      originUrl,
                                                                                      storage,
                                                                                      Optional.empty(),
                                                                                      "groupId",
                                                                                      "sessionOwner",
                                                                                      "session",
                                                                                      false);
            request.setStatus(StorageRequestStatus.TO_HANDLE);
            createdStorageRequests.add(request);
        }
        return this.fileStorageRequestAggregationRepository.saveAll(createdStorageRequests);
    }

    private FileReference createFileReference(String storage) {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       "file.test",
                                                                       100L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        FileLocation location = new FileLocation(storage, "anywhere://in/this/directory/" + checksum, null);

        return fileReferenceRepository.save(new FileReference("owner", fileMetaInfo, location));
    }
}
