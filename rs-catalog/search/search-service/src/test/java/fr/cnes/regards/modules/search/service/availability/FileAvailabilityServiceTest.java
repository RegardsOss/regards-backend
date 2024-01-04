/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.availability;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import feign.FeignException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.filecatalog.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.filecatalog.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.dto.availability.FilesAvailabilityResponseDto;
import fr.cnes.regards.modules.search.dto.availability.ProductFilesStatusDto;
import fr.cnes.regards.modules.search.service.AccessStatus;
import fr.cnes.regards.modules.search.service.CatalogSearchService;
import fr.cnes.regards.modules.search.service.accessright.AccessRightFilterException;
import fr.cnes.regards.modules.search.service.accessright.DataAccessRightService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@RunWith(MockitoJUnitRunner.class)
public class FileAvailabilityServiceTest {

    @Mock
    private CatalogSearchService catalogSearchService;

    @Mock
    private IStorageRestClient storageRestClient;

    @Mock
    private DataAccessRightService dataAccessRightService;

    @InjectMocks
    private FileAvailabilityService fileAvailabilityService;

    @Captor
    private ArgumentCaptor<Object> recordsCaptor;

    private static final UniformResourceName PRODUCT_1 = buildURN("file1");

    private static final UniformResourceName PRODUCT_2 = buildURN("file2");

    private static final UniformResourceName DATAOBJECT_URN1 = UniformResourceName.fromString(
        "URN:AIP:DATA:PROJECT:f8cc114a-3ce2-401f-9769-1f890e75a166:V1");

    private static final UniformResourceName DATAOBJECT_URN2 = UniformResourceName.fromString(
        "URN:AIP:DATA:PROJECT:c1ced654-cf80-4ef0-b4c7-0dfb8140962a:V2");

    private static final UniformResourceName DATAOBJECT_URN3 = UniformResourceName.fromString(
        "URN:AIP:DATA:PROJECT:47b08ff5-a0a2-4c2d-a0c7-196426cae87a:V3");

    @Before
    public void init() {
        // common mock : consider all files as available
        try {
            Mockito.when(dataAccessRightService.checkContentAccess(Mockito.any())).thenReturn(AccessStatus.GRANTED);
        } catch (AccessRightFilterException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        // field annotated @Value are not completed because we don't use spring
        // we need to set these configurable properties manually
        ReflectionTestUtils.setField(fileAvailabilityService, "maxBulkSize", 100);
    }

    @Test
    public void test_checksums_well_sent() throws ModuleException {
        // GIVEN 3 files distributed in 2 dataObject
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN2, DATAOBJECT_URN3));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        // don't care about return values of storage here, we control only given params of the storage client
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any())).thenReturn(ResponseEntity.ok(List.of()));

        ArgumentCaptor<FilesAvailabilityRequestDto> requestCaptor = ArgumentCaptor.forClass(FilesAvailabilityRequestDto.class);

        // WHEN check availability to these 2 products
        fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(), PRODUCT_2.toString()));

        // THEN check if storage request contains these 3 files (checksums)
        Mockito.verify(storageRestClient, Mockito.times(1)).checkFileAvailability(requestCaptor.capture());
        FilesAvailabilityRequestDto requestSent = requestCaptor.getAllValues().get(0);
        Assertions.assertEquals(3, requestSent.getChecksums().size());
        Assertions.assertTrue(requestSent.getChecksums().contains(DATAOBJECT_URN1.toString()));
        Assertions.assertTrue(requestSent.getChecksums().contains(DATAOBJECT_URN2.toString()));
        Assertions.assertTrue(requestSent.getChecksums().contains(DATAOBJECT_URN3.toString()));
    }

    @Test
    public void test_same_checksum_on_multiple_files() throws ModuleException {
        // GIVEN 3 files duplicated in 2 dataObjects
        // don't care about return values of storage here. We control only given params of the storage client
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any())).thenReturn(ResponseEntity.ok(List.of()));
        ArgumentCaptor<FilesAvailabilityRequestDto> requestCaptor = ArgumentCaptor.forClass(FilesAvailabilityRequestDto.class);
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1, DATAOBJECT_URN2),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN1, DATAOBJECT_URN2));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        //WHEN requesting only 2 products
        fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(), PRODUCT_2.toString()));

        // THEN only these 2 files are asked to storage
        Mockito.verify(storageRestClient, Mockito.times(1)).checkFileAvailability(requestCaptor.capture());
        FilesAvailabilityRequestDto requestSent = requestCaptor.getAllValues().get(0);
        Assertions.assertEquals(2, requestSent.getChecksums().size());
    }

    @Test
    public void test_nominal_return_of_storage() throws ModuleException {
        // GIVEN 3 files separated in 2 products
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN2, DATAOBJECT_URN3));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        // Mock storage response OK
        // providerID, checksum and urn are identical for this test
        List<FileAvailabilityStatusDto> storageResult = List.of(new FileAvailabilityStatusDto(DATAOBJECT_URN1.toString(),
                                                                                              true,
                                                                                              null),
                                                                new FileAvailabilityStatusDto(DATAOBJECT_URN2.toString(),
                                                                                              false,
                                                                                              null),
                                                                new FileAvailabilityStatusDto(DATAOBJECT_URN3.toString(),
                                                                                              true,
                                                                                              OffsetDateTime.MAX));
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any()))
               .thenReturn(ResponseEntity.ok(storageResult));
        //WHEN request only 2 files
        FilesAvailabilityResponseDto response = fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(),
                                                                                                 PRODUCT_2.toString()));
        Assertions.assertEquals(2, response.getProducts().size());
        // THEN Control file statuses
        ProductFilesStatusDto product1Status = response.getProducts()
                                                       .stream()
                                                       .filter(status -> PRODUCT_1.toString().equals(status.getId()))
                                                       .findFirst()
                                                       .get();
        ProductFilesStatusDto product2Status = response.getProducts()
                                                       .stream()
                                                       .filter(status -> PRODUCT_2.toString().equals(status.getId()))
                                                       .findFirst()
                                                       .get();
        Assertions.assertEquals(1, product1Status.getFiles().size());
        Assertions.assertEquals(2, product2Status.getFiles().size());
    }

    @Test
    public void test_nominal_return_of_storage_with_file_duplicated_in_few_products() throws ModuleException {
        // GIVEN 3 files for 2 products, DATAOBJECT_URN_2 duplicated in 2 products
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1, DATAOBJECT_URN2),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN2, DATAOBJECT_URN3));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        // Mock storage response OK
        // providerID, checksum and urn are identical for this test
        List<FileAvailabilityStatusDto> storageResult = List.of(new FileAvailabilityStatusDto(DATAOBJECT_URN1.toString(),
                                                                                              true,
                                                                                              null),
                                                                new FileAvailabilityStatusDto(DATAOBJECT_URN2.toString(),
                                                                                              false,
                                                                                              null),
                                                                new FileAvailabilityStatusDto(DATAOBJECT_URN3.toString(),
                                                                                              true,
                                                                                              OffsetDateTime.MAX));
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any()))
               .thenReturn(ResponseEntity.ok(storageResult));
        // WHEN request only 2 files
        FilesAvailabilityResponseDto response = fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(),
                                                                                                 PRODUCT_2.toString()));
        Assertions.assertEquals(2, response.getProducts().size());
        // THEN Control file statuses
        ProductFilesStatusDto product1Status = response.getProducts()
                                                       .stream()
                                                       .filter(status -> PRODUCT_1.toString().equals(status.getId()))
                                                       .findFirst()
                                                       .get();
        ProductFilesStatusDto product2Status = response.getProducts()
                                                       .stream()
                                                       .filter(status -> PRODUCT_2.toString().equals(status.getId()))
                                                       .findFirst()
                                                       .get();
        // Each product status have 2 files, even if file checksum is the same for 1 file.
        Assertions.assertEquals(2, product1Status.getFiles().size());
        Assertions.assertEquals(2, product2Status.getFiles().size());
    }

    @Test
    public void test_degraded_return_of_storage() throws ModuleException {
        // GIVEN 3 files for 2 products, DATAOBJECT_URN_2 duplicated in 2 products
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN2, DATAOBJECT_URN3));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        // Mock storage response OK
        // providerID, checksum and urn are identical for this test
        List<FileAvailabilityStatusDto> storageResult = List.of(new FileAvailabilityStatusDto(DATAOBJECT_URN1.toString(),
                                                                                              true,
                                                                                              null),
                                                                new FileAvailabilityStatusDto(DATAOBJECT_URN2.toString(),
                                                                                              false,
                                                                                              null),
                                                                new FileAvailabilityStatusDto(DATAOBJECT_URN3.toString(),
                                                                                              true,
                                                                                              OffsetDateTime.MAX));
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any()))
               .thenThrow(FeignException.UnprocessableEntity.class);
        // WHEN request only 2 files
        try {
            FilesAvailabilityResponseDto response = fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(),
                                                                                                     PRODUCT_2.toString()));
            Assertions.fail("check availability call must failed");
            // THEN availability method must fail and throw
        } catch (Exception e) {
            Assertions.assertEquals(ModuleException.class, e.getClass());
            Assertions.assertTrue(e.getMessage().contains("access to rs-storage failed"));
        }
    }

    @Test
    public void test_degraded_storage_not_available() throws ModuleException {
        // GIVEN 3 files for 2 products, DATAOBJECT_URN_2 duplicated in 2 products
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN2, DATAOBJECT_URN3));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any()))
               .thenThrow(FeignException.ServiceUnavailable.class);
        // WHEN request only 2 files
        try {
            FilesAvailabilityResponseDto response = fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(),
                                                                                                     PRODUCT_2.toString()));
            Assertions.fail("check availability call must fail");
        } catch (Exception e) {
            // THEN availability method must fail and throw
            Assertions.assertEquals(ModuleException.class, e.getClass());
            Assertions.assertTrue(e.getMessage().contains("access to rs-storage failed"));
        }
    }

    @Test
    public void test_degraded_too_much_files() throws ModuleException {
        // GIVEN 3 files for 2 products, DATAOBJECT_URN_2 duplicated in 2 products
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1),
                                                buildProductWithFiles(PRODUCT_2, DATAOBJECT_URN2, DATAOBJECT_URN3));
        // set 110 files reattached to a product
        searchResult.get(0)
                    .getFiles()
                    .get(DataType.RAWDATA)
                    .addAll(IntStream.range(0, 110).mapToObj(i -> buildDataFile(buildRandomURN())).toList());
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        // WHEN request only 2 files
        try {
            FilesAvailabilityResponseDto response = fileAvailabilityService.checkAvailability(Set.of(PRODUCT_1.toString(),
                                                                                                     PRODUCT_2.toString()));
            Assertions.fail("check availability call must failed");
        } catch (FileAvailabilityException e) {
            // THEN availability method must fail and throw
            Assertions.assertTrue(e.getMessage().contains("Too many files"));
            // current request contains 113 files
            Assertions.assertTrue(e.getMessage().contains("113 files"));
        }
    }

    @Test
    public void test_single_product_availability_ok() throws ModuleException {
        // GIVEN 1 files for 1 product
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);
        // Mock storage response OK
        // providerID, checksum and urn are identical for this test
        List<FileAvailabilityStatusDto> storageResult = List.of(new FileAvailabilityStatusDto(DATAOBJECT_URN1.toString(),
                                                                                              true,
                                                                                              null));
        Mockito.when(storageRestClient.checkFileAvailability(Mockito.any()))
               .thenReturn(ResponseEntity.ok(storageResult));
        // WHEN request only this product
        ProductFilesStatusDto response = fileAvailabilityService.checkAvailability(PRODUCT_1.toString());
        // THEN everything's ok
        Assertions.assertEquals(1, response.getFiles().size());
        Assertions.assertEquals(PRODUCT_1.toString(), response.getId());
        Assertions.assertEquals(DATAOBJECT_URN1.toString(),
                                response.getFiles().stream().findFirst().get().getChecksum());
        Assertions.assertTrue(response.getFiles().stream().findFirst().get().isAvailable());
    }

    @Test
    public void test_single_product_availability_not_found() throws ModuleException {
        // GIVEN no file
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(List.of());
        try {
            // WHEN request only 1 product which not exists
            ProductFilesStatusDto response = fileAvailabilityService.checkAvailability(PRODUCT_1.toString());
            Assertions.fail("check availability call must failed");
        } catch (FileAvailabilityException e) {
            // THEN return NOT FOUND
            Assertions.assertEquals(NotAvailabilityCauseEnum.NOT_FOUND, e.getNotAvailabilityCause());
        }
    }

    @Test
    public void test_single_product_availability_not_access_right() throws ModuleException, ExecutionException {
        // GIVEN 1 files for 1 product
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, DATAOBJECT_URN1));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);

        // mock access right to FORBIDDEN
        Mockito.when(dataAccessRightService.checkContentAccess(Mockito.any())).thenReturn(AccessStatus.FORBIDDEN);
        try {
            // WHEN requesting availability of this product
            ProductFilesStatusDto response = fileAvailabilityService.checkAvailability(PRODUCT_1.toString());
            Assertions.fail("check availability call must failed");
        } catch (FileAvailabilityException e) {
            // THEN availability method must fail and throw
            Assertions.assertEquals(NotAvailabilityCauseEnum.FORBIDDEN, e.getNotAvailabilityCause());
        }

        // mock access right to NOT FOUND
        Mockito.when(dataAccessRightService.checkContentAccess(Mockito.any())).thenReturn(AccessStatus.NOT_FOUND);
        try {
            // WHEN requesting availability of this product
            ProductFilesStatusDto response = fileAvailabilityService.checkAvailability(PRODUCT_1.toString());
            Assertions.fail("check availability call must failed");
        } catch (FileAvailabilityException e) {
            // THEN availability method must fail and throw
            Assertions.assertEquals(NotAvailabilityCauseEnum.FORBIDDEN, e.getNotAvailabilityCause());
        }
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private UniformResourceName buildRandomURN() {
        return UniformResourceName.build("id", EntityType.DATA, "id", UUID.randomUUID(), 1);
    }

    private DataObject buildProductWithFiles(UniformResourceName id, UniformResourceName... files) {
        // create files
        Multimap<DataType, DataFile> fileMultimap = ArrayListMultimap.create();
        Arrays.stream(files).forEach(urn -> fileMultimap.put(DataType.RAWDATA, buildDataFile(urn)));
        // create dataObjectFeature and attach files
        DataObjectFeature feature = new DataObjectFeature(id, id.toString(), id.toString());
        feature.setFiles(fileMultimap);
        // create dataObject and attach feature
        DataObject dataObject = new DataObject();
        dataObject.setFeature(feature);
        return dataObject;
    }

    private DataFile buildDataFile(UniformResourceName urn) {
        DataFile dataFile = new DataFile();
        dataFile.setOnline(false);
        try {
            dataFile.setUri(new URI("file:///test/" + urn).toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        dataFile.setFilename(urn.toString());
        dataFile.setFilesize(10L);
        dataFile.setReference(false);
        dataFile.setChecksum(urn.toString());
        dataFile.setDigestAlgorithm("MD5");
        dataFile.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        dataFile.setDataType(DataType.RAWDATA);
        return dataFile;
    }

    private static UniformResourceName buildURN(String id) {
        return UniformResourceName.build(id, EntityType.DATA, "tenant_test", UUID.randomUUID(), 1);
    }
}
