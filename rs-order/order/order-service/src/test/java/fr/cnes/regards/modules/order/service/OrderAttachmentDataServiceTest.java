/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.dam.client.entities.IDatasetClient;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;

/**
 * @author Thomas GUILLOU
 **/
public class OrderAttachmentDataServiceTest {

    @MockBean
    private IDatasetClient datasetClient;

    private OrderAttachmentDataSetService orderAttachmentDataSetService;

    private static final String DAM_FILE_NAME = "dam_file";

    private static final String EXTERNAL_FILE_NAME = "external_file";

    private static final String STORAGE_FILE_NAME = "storage_file";

    @Before
    public void before() {
        OrderHelperService orderHelperService = new OrderHelperService(null, null, null, null, null, null, null);
        datasetClient = Mockito.mock(IDatasetClient.class);
        orderAttachmentDataSetService = new OrderAttachmentDataSetService(datasetClient, orderHelperService, null);
    }

    @Test
    public void testFillBucketsWithDataSetFiles() throws ModuleException {
        // GIVEN
        Order order = new Order();
        order.setId(1L);
        order.setCreationDate(OffsetDateTime.now());
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        Dataset dataset = createDataSet();
        dataset.getFeature().getFiles().get(DataType.RAWDATA).add(createDataFileStoredInDam());
        dataset.getFeature().getFiles().get(DataType.RAWDATA).add(createDataFileStoredInStorage());
        dataset.getFeature().getFiles().get(DataType.RAWDATA).add(createDataFileStoredExternally());
        ResponseEntity<Dataset> datasetResponseEntity = new ResponseEntity<>(dataset, HttpStatus.OK);

        Mockito.when(datasetClient.retrieveDataset(Mockito.nullable(String.class))).thenReturn(datasetResponseEntity);
        BasketDatasetSelection dsSel = new BasketDatasetSelection();

        // WHEN
        Set<OrderDataFile> storageBucket = new HashSet<>();
        Set<OrderDataFile> externalBucket = new HashSet<>();
        orderAttachmentDataSetService.fillBucketsWithDataSetFiles(order, dsSel, storageBucket, externalBucket);

        // THEN
        Assertions.assertEquals(2, externalBucket.size());
        Assertions.assertEquals(1, storageBucket.size());
        List<String> externalBucketFileNames = externalBucket.stream().map(OrderDataFile::getFilename).toList();
        List<String> storageBucketFileNames = storageBucket.stream().map(OrderDataFile::getFilename).toList();
        Assertions.assertTrue(externalBucketFileNames.containsAll(List.of(DAM_FILE_NAME, EXTERNAL_FILE_NAME)));
        Assertions.assertTrue(storageBucketFileNames.contains(STORAGE_FILE_NAME));
    }

    private Dataset createDataSet() {
        OaisUniformResourceName urn = new OaisUniformResourceName(OAISIdentifier.AIP,
                                                                  EntityType.DATASET,
                                                                  "tenant",
                                                                  UUID.randomUUID(),
                                                                  1,
                                                                  null,
                                                                  null);
        DatasetFeature datasetFeature = new DatasetFeature(urn, "providerId", "label", "licence");
        Dataset dataset = new Dataset();
        dataset.setFeature(datasetFeature);
        return dataset;
    }

    private static DataFile createDataFileStoredInDam() {
        return createDataFile(DAM_FILE_NAME, "www.regards.fr/urlMustEndsWith/dam", false);
    }

    private static DataFile createDataFileStoredInStorage() {
        return createDataFile(STORAGE_FILE_NAME, "www.regards.fr/anyUrl", false);
    }

    private static DataFile createDataFileStoredExternally() {
        return createDataFile(EXTERNAL_FILE_NAME, "www.regards.fr/anyUrl", true);
    }

    private static DataFile createDataFile(String filename, String uri, boolean isReference) {
        DataFile dataFile = new DataFile();
        dataFile.setChecksum(filename);
        dataFile.setDataType(DataType.RAWDATA);
        dataFile.setFilename(filename);
        dataFile.setMimeType(MimeTypeUtils.parseMimeType("application/octet-stream"));
        dataFile.setOnline(true);
        dataFile.setUri(uri);
        dataFile.setReference(isReference);
        dataFile.setFilesize(67170l);
        return dataFile;
    }
}
