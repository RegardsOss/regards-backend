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
package fr.cnes.regards.modules.crawler.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.crawler.service.exception.ElasticSearchSaveBulkException;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionService;
import fr.cnes.regards.modules.crawler.service.service.IngestionParameters;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourceException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author tguillou
 */
@Plugin(id = "TestDataSourcePluginFailable",
        version = "1.0-SNAPSHOT",
        description = "Test DataSource Plugin that fail on command",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestDataSourcePluginFailable implements IDataSourcePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataSourcePluginFailable.class);

    @Override
    public String getModelName() {
        return "model";
    }

    @Override
    public int getRefreshRate() {
        return 0;
    }

    private static int findAllCpt = 0;

    public static int totalDataObjectsToCreate = 0;

    private static DatasourceIngestionService datasourceIngestionService;

    public static int dataObjectBulkSize = 0;

    public static List<Integer> bulkToFail = new ArrayList<>();

    public static List<Integer> saveCallToFail = new ArrayList<>();

    public static void configureFailAtSaveCalls(int... i) {
        saveCallToFail = Arrays.stream(i).boxed().collect(Collectors.toList());
    }

    public static int getFindAllCpt() {
        return findAllCpt;
    }

    /**
     * Wait 3 seconds before returning data objects for the given calls.
     */
    public static void configureLongTaskAtSaveCalls(int... callsToWait) {
        try {
            List<Integer> callsList = Arrays.stream(callsToWait).boxed().toList();
            AtomicInteger callCount = new AtomicInteger(0);
            Mockito.doAnswer(invocation -> {
                       int currentCall = callCount.incrementAndGet();
                       if (callsList.contains(currentCall)) {
                           LOGGER.warn("Must wait for call {}", currentCall);
                           IntStream.range(0, 3).forEach(cpt -> {
                               try {
                                   LOGGER.warn("Has waited for {} seconds before returning data objects for call {}",
                                               cpt,
                                               currentCall);
                                   Thread.sleep(1000);
                               } catch (InterruptedException e) {
                                   Thread.currentThread().interrupt();
                               }
                           });
                           LOGGER.warn("Waited for 3 seconds before returning data objects for call {}", currentCall);
                       } else {
                           LOGGER.warn("No wait for call {}", currentCall);
                       }
                       if (saveCallToFail.contains(currentCall)) {
                           LOGGER.warn("Force fail save call number {}", currentCall);
                           throw new ElasticSearchSaveBulkException();
                       }
                       return invocation.callRealMethod();
                   })
                   .when(datasourceIngestionService)
                   .createOrMergeDataObjects(Mockito.any(IngestionParameters.class),
                                             Mockito.anyString(),
                                             Mockito.anyList(),
                                             Mockito.anyBoolean());
        } catch (Exception e) {
            throw new RuntimeException("Error configuring long task at calls", e);
        }
    }

    public static void configureIngestion(int dataObjectBulkSize,
                                          int totalDataObjectsToCreate,
                                          DatasourceIngestionService datasourceIngestionService) {
        TestDataSourcePluginFailable.dataObjectBulkSize = dataObjectBulkSize;
        TestDataSourcePluginFailable.totalDataObjectsToCreate = totalDataObjectsToCreate;
        TestDataSourcePluginFailable.datasourceIngestionService = datasourceIngestionService;
        findAllCpt = 0;
        bulkToFail.clear();
        saveCallToFail.clear();
    }

    public static void configureFailAtFindAllCall(int i) {
        bulkToFail.add(i);
    }

    @Override
    public List<DataObjectFeature> findAll(String tenant,
                                           CrawlingCursor cursor,
                                           @Nullable OffsetDateTime lastIngestDate,
                                           OffsetDateTime currentIngestionStartDate) throws DataSourceException {
        findAllCpt++;
        if (bulkToFail.contains(findAllCpt)) {
            throw new DataSourceException("TestDataSourcePluginFailable failed on command for findAll call number "
                                          + findAllCpt);
        }
        try {
            Thread.sleep(20); // micro delay to simulate a "real" findAll call
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<DataObjectFeature> dataObjectsRes;
        if ((findAllCpt - 1) * dataObjectBulkSize >= totalDataObjectsToCreate) {
            cursor.setHasNext(false);
            dataObjectsRes = List.of();
        } else {
            cursor.setHasNext(true);
            dataObjectsRes = IntStream.range(0, dataObjectBulkSize)
                                      .mapToObj(i -> DataObjectGenerator.generate())
                                      .toList();
        }
        return dataObjectsRes;
    }

    /**
     * Generate a random urn for testing purposes.
     */
    static class DataObjectGenerator {

        public static int cpt = 0;

        public static DataObjectFeature generate() {
            cpt++;
            String providerId = "providerId" + getFormattedValue();
            return new DataObjectFeature(generateUrn(), providerId, providerId);
        }

        private static UniformResourceName generateUrn() {
            return UniformResourceName.fromString(String.format("URN:AIP:DATA:tenant:%s-0000-0000-0000-c08ce481809d:V1",
                                                                getFormattedValue()));
        }

        /**
         * Increments the counter and returns its value as a zero-padded 8-digit string.
         * For example: 0 -> "00000000", 1 -> "00000001", 12 -> "00000012"
         *
         * @return the formatted counter value
         */
        private static String getFormattedValue() {
            return String.format("%08d", cpt);
        }
    }
}
