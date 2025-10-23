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
package fr.cnes.regards.modules.search.service.restoration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.service.CatalogSearchService;
import fr.cnes.regards.modules.search.service.accessright.DataAccessRightService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * @author Stephane Cortine
 */
@RunWith(MockitoJUnitRunner.class)
public class FileRestoreServiceTest {

    private static final int AVAILABILITY_HOURS = 24;

    private static final UniformResourceName PRODUCT_1 = buildURN("file1");

    private static final UniformResourceName PRODUCT_2 = buildURN("file2");

    @Mock
    private CatalogSearchService catalogSearchService;

    @Mock
    private IStorageClient storageClient;

    @Mock
    private DataAccessRightService dataAccessRightService;

    @InjectMocks
    private FileRestoreService fileRestoreService;

    @Before
    public void init() {
        // field annotated @Value are not completed because we don't use spring
        // we need to set these configurable properties manually
        ReflectionTestUtils.setField(fileRestoreService, "maxBulkSize", 100);
        ReflectionTestUtils.setField(fileRestoreService, "availabilityHours", AVAILABILITY_HOURS);
    }

    @Test
    public void test_restore_one_product() throws ModuleException, ExecutionException {
        // Given
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, "file1.txt", "file2.txt"));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);

        Mockito.when(dataAccessRightService.removeProductsWhereAccessRightNotGranted(Mockito.any()))
               .thenReturn(searchResult);

        Mockito.when(storageClient.makeAvailable(Mockito.any(), Mockito.anyInt())).thenReturn(List.of());

        // When
        fileRestoreService.restore(PRODUCT_1.toString());

        // Then
        ArgumentCaptor<Collection<String>> checksumsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Integer> availabilityHoursCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(storageClient, Mockito.times(1))
               .makeAvailable(checksumsCaptor.capture(), availabilityHoursCaptor.capture());

        Assertions.assertEquals(2, checksumsCaptor.getValue().size());
        Assertions.assertEquals(AVAILABILITY_HOURS, availabilityHoursCaptor.getValue());
    }

    @Test
    public void test_restore_one_product_not_found() throws ModuleException, ExecutionException {
        // Given
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(List.of());

        // When
        try {
            fileRestoreService.restore(PRODUCT_1.toString());

            Assertions.fail("ask restoration call must failed");
        } catch (FileRestoreException e) {
            // Then method must fail and throw
            Assertions.assertTrue(e.getMessage().contains("Product " + PRODUCT_1.toString() + " cannot be found"));
        }
        // Then
        Mockito.verify(dataAccessRightService, Mockito.never()).removeProductsWhereAccessRightNotGranted(Mockito.any());
        Mockito.verify(storageClient, Mockito.never()).makeAvailable(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void test_restore_one_product_not_access_file() throws ModuleException, ExecutionException {
        // Given
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, "file1.txt", "file2.txt"));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);

        Mockito.when(dataAccessRightService.removeProductsWhereAccessRightNotGranted(Mockito.any()))
               .thenReturn(List.of());

        // When
        try {
            fileRestoreService.restore(PRODUCT_1.toString());

            Assertions.fail("ask restoration call must failed");
        } catch (FileRestoreException e) {
            // Then method must fail and throw
            Assertions.assertTrue(e.getMessage()
                                   .contains("Current user has not access to product " + PRODUCT_1.toString() + "."));
        }
        // Then
        Mockito.verify(storageClient, Mockito.never()).makeAvailable(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void test_restore_several_products() throws ModuleException, ExecutionException {
        // Given
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, "file1.txt", "file2.txt"),
                                                buildProductWithFiles(PRODUCT_2, "file3.txt", "file4.txt"));
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);

        Mockito.when(dataAccessRightService.removeProductsWhereAccessRightNotGranted(Mockito.any()))
               .thenReturn(searchResult);

        Mockito.when(storageClient.makeAvailable(Mockito.any(), Mockito.anyInt())).thenReturn(List.of());

        // When
        fileRestoreService.restore(Set.of(PRODUCT_1.toString(), PRODUCT_2.toString()));

        // Then
        ArgumentCaptor<Collection<String>> checksumsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Integer> availabilityHoursCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(storageClient, Mockito.times(1))
               .makeAvailable(checksumsCaptor.capture(), availabilityHoursCaptor.capture());

        Assertions.assertEquals(4, checksumsCaptor.getValue().size());
        Assertions.assertEquals(AVAILABILITY_HOURS, availabilityHoursCaptor.getValue());
    }

    @Test
    public void test_restore_several_products_with_no_access_file() throws ModuleException, ExecutionException {
        // Given
        DataObject dataAccessFile = buildProductWithFiles(PRODUCT_1, "file1.txt", "file2.txt");
        DataObject dataNoAccessFile = buildProductWithFiles(PRODUCT_2, "file3.txt", "file4.txt");
        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any()))
               .thenReturn(List.of(dataAccessFile, dataNoAccessFile));
        // Return only data with access right
        Mockito.when(dataAccessRightService.removeProductsWhereAccessRightNotGranted(Mockito.any()))
               .thenReturn(List.of(dataAccessFile));

        Mockito.when(storageClient.makeAvailable(Mockito.any(), Mockito.anyInt())).thenReturn(List.of());

        // When
        fileRestoreService.restore(Set.of(PRODUCT_1.toString(), PRODUCT_2.toString()));

        // Then
        ArgumentCaptor<Collection<String>> checksumsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Integer> availabilityHoursCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(storageClient, Mockito.times(1))
               .makeAvailable(checksumsCaptor.capture(), availabilityHoursCaptor.capture());

        Assertions.assertEquals(2, checksumsCaptor.getValue().size());
        Assertions.assertEquals(AVAILABILITY_HOURS, availabilityHoursCaptor.getValue());
    }

    @Test
    public void test_restore_several_products_with_too_much_files() throws ModuleException, ExecutionException {
        // Given
        List<DataObject> searchResult = List.of(buildProductWithFiles(PRODUCT_1, "file1.txt"),
                                                buildProductWithFiles(PRODUCT_2, "file2.txt"));
        searchResult.get(0)
                    .getFiles()
                    .get(DataType.RAWDATA)
                    .addAll(IntStream.range(0, 99).mapToObj(i -> buildDataFile("file0" + i + ".txt")).toList());

        Mockito.when(catalogSearchService.searchByUrnIn(Mockito.any())).thenReturn(searchResult);

        Mockito.when(dataAccessRightService.removeProductsWhereAccessRightNotGranted(Mockito.any()))
               .thenReturn(searchResult);

        // When
        try {
            fileRestoreService.restore(Set.of(PRODUCT_1.toString(), PRODUCT_2.toString()));

            Assertions.fail("ask restoration call must failed");
        } catch (FileRestoreException e) {
            // Then method must fail and throw
            Assertions.assertTrue(e.getMessage().contains("Too many files"));
            Assertions.assertTrue(e.getMessage().contains("101 files"));
        }
        // Then
        Mockito.verify(storageClient, Mockito.never()).makeAvailable(Mockito.any(), Mockito.anyInt());
    }

    private static UniformResourceName buildURN(String id) {
        return UniformResourceName.build(id, EntityType.DATA, "tenant_test", UUID.randomUUID(), 1);
    }

    private DataObject buildProductWithFiles(UniformResourceName id, String... files) {
        // create files
        Multimap<DataType, DataFile> fileMultimap = ArrayListMultimap.create();
        Arrays.stream(files).forEach(file -> fileMultimap.put(DataType.RAWDATA, buildDataFile(file)));
        // create dataObjectFeature and attach files
        DataObjectFeature feature = new DataObjectFeature(id, id.toString(), id.toString());
        feature.setFiles(fileMultimap);
        // create dataObject and attach feature
        DataObject dataObject = new DataObject();
        dataObject.setFeature(feature);

        return dataObject;
    }

    private DataFile buildDataFile(String file) {
        DataFile dataFile = new DataFile();
        dataFile.setOnline(false);
        dataFile.setUri("file:///test/" + file);
        dataFile.setFilename(file);
        dataFile.setFilesize(10L);
        dataFile.setReference(false);
        try {
            dataFile.setChecksum(ChecksumUtils.computeHexChecksum(file, "MD5"));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
        dataFile.setDigestAlgorithm("MD5");
        dataFile.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        dataFile.setDataType(DataType.RAWDATA);

        return dataFile;
    }

}
