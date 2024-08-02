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

package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureFileUpdateMode;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;

public class FeatureFilesServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureFilesServiceTest.class);

    private IStorageClient storageClient;

    private FeatureFilesService featureFilesService;

    private static final String URL_1 = "URL_1";

    private static final String STAF_STORAGE = "STAF";

    private final FeatureFileSampleFactory featureFileSampleFactory1 = new FeatureFileSampleFactory("FILE_1",
                                                                                                    1000L,
                                                                                                    "CHECKSUM_1",
                                                                                                    URL_1);

    // Same as above but with another storage
    private final FeatureFileSampleFactory featureFileSampleFactory1_storage = new FeatureFileSampleFactory("FILE_1",
                                                                                                            1000L,
                                                                                                            "CHECKSUM_1",
                                                                                                            URL_1,
                                                                                                            STAF_STORAGE);

    private final FeatureFileSampleFactory featureFileSampleFactory2 = new FeatureFileSampleFactory("FILE_2",
                                                                                                    2000L,
                                                                                                    "CHECKSUM_2",
                                                                                                    "URL_2");

    private static final String FILE_3_NEW = "FILE_3_NEW";

    private static final String URL_3_NEW = "URL_3_NEW";

    private final FeatureFileSampleFactory featureFileSampleFactory3 = new FeatureFileSampleFactory("FILE_3",
                                                                                                    3000L,
                                                                                                    "CHECKSUM_3",
                                                                                                    "URL_3");

    private final FeatureFileSampleFactory featureFileSampleFactory3_new = new FeatureFileSampleFactory(FILE_3_NEW,
                                                                                                        3000L,
                                                                                                        "CHECKSUM_3",
                                                                                                        URL_3_NEW);

    @Before
    public void setUp() {
        storageClient = Mockito.mock(IStorageClient.class);
        featureFilesService = new FeatureFilesService(storageClient);
    }

    @Test
    public void whenReplaceModeWithSameFiles_thenUpdateWithoutChange() {
        // Given
        FeatureUpdateRequest request = buildFeatureUpdateRequest(featureFileSampleFactory1.buildFeatureFile(),
                                                                 featureFileSampleFactory2.buildFeatureFile(),
                                                                 featureFileSampleFactory3.buildFeatureFile());
        FeatureEntity entity = buildFeatureEntity(featureFileSampleFactory1.buildFeatureFile(),
                                                  featureFileSampleFactory2.buildFeatureFile(),
                                                  featureFileSampleFactory3.buildFeatureFile());
        List<RequestResultInfoDto> results = Arrays.asList(featureFileSampleFactory1.buildStorageResult(entity),
                                                           featureFileSampleFactory2.buildStorageResult(entity),
                                                           featureFileSampleFactory3.buildStorageResult(entity));

        // When
        simulateFileUpdate(request, entity, results);

        // Then
        Assert.assertEquals(3, entity.getFeature().getFiles().size());
        Mockito.verify(storageClient, Mockito.never()).delete(Mockito.anyCollection());
    }

    @Test
    public void whenReplaceModeWithDifferentFiles_thenUpdateAndClean() {
        // Given
        FeatureUpdateRequest request = buildFeatureUpdateRequest(featureFileSampleFactory1.buildFeatureFile(),
                                                                 featureFileSampleFactory2.buildFeatureFile());
        FeatureEntity entity = buildFeatureEntity(featureFileSampleFactory3.buildFeatureFile());
        List<RequestResultInfoDto> results = Arrays.asList(featureFileSampleFactory1.buildStorageResult(entity),
                                                           featureFileSampleFactory2.buildStorageResult(entity));

        // When
        simulateFileUpdate(request, entity, results);

        // Then
        Assert.assertEquals(2, entity.getFeature().getFiles().size());
        Mockito.verify(storageClient, Mockito.times(1)).delete(Mockito.anyCollection());
    }

    @Test
    public void whenReplaceModeWithDifferentUrls_thenUpdateAndClean() {
        // Given
        FeatureUpdateRequest request = buildFeatureUpdateRequest(featureFileSampleFactory3_new.buildFeatureFile());
        FeatureEntity entity = buildFeatureEntity(featureFileSampleFactory3.buildFeatureFile());
        List<RequestResultInfoDto> results = Collections.singletonList(featureFileSampleFactory3_new.buildStorageResult(
            entity));

        // When
        simulateFileUpdate(request, entity, results);

        // Then
        List<FeatureFile> featureFiles = entity.getFeature().getFiles();
        Assert.assertEquals(1, featureFiles.size());
        FeatureFile featureFile = featureFiles.get(0);
        Set<FeatureFileLocation> locations = featureFile.getLocations();
        Assert.assertEquals(1, locations.size());
        FeatureFileLocation featureFileLocation = locations.stream().findFirst().orElseThrow();
        Assert.assertEquals(URL_3_NEW, featureFileLocation.getUrl());
        Assert.assertEquals(FILE_3_NEW, featureFile.getAttributes().getFilename());

        Mockito.verify(storageClient, Mockito.never()).delete(Mockito.anyCollection());
    }

    @Test
    public void whenReplaceModeWithDifferentStorage_thenCleanOldStorage() {
        // Given
        FeatureUpdateRequest request = buildFeatureUpdateRequest(featureFileSampleFactory1_storage.buildFeatureFile());
        FeatureEntity entity = buildFeatureEntity(featureFileSampleFactory1.buildFeatureFile());
        List<RequestResultInfoDto> results =
            Collections.singletonList(featureFileSampleFactory1_storage.buildStorageResult(
            entity));

        // When
        simulateFileUpdate(request, entity, results);

        // Then
        List<FeatureFile> featureFiles = entity.getFeature().getFiles();
        Assert.assertEquals(1, featureFiles.size());
        FeatureFile featureFile = featureFiles.get(0);
        Set<FeatureFileLocation> locations = featureFile.getLocations();
        Assert.assertEquals(1, locations.size());
        FeatureFileLocation featureFileLocation = locations.stream().findFirst().orElseThrow();
        Assert.assertEquals(STAF_STORAGE, featureFileLocation.getStorage());
        Assert.assertEquals(URL_1, featureFileLocation.getUrl());

        Mockito.verify(storageClient, Mockito.timeout(1)).delete(Mockito.anyCollection());

    }

    private FeatureUpdateRequest buildFeatureUpdateRequest(FeatureFile... featureFiles) {
        Feature feature = initFeature();
        if (featureFiles != null) {
            for (FeatureFile featureFile : featureFiles) {
                feature.getFiles().add(featureFile);
            }
        }
        FeatureUpdateRequest request = FeatureUpdateRequest.build("requestId",
                                                                  "requestOwner",
                                                                  OffsetDateTime.now(),
                                                                  RequestState.GRANTED,
                                                                  Sets.newHashSet(),
                                                                  feature,
                                                                  PriorityLevel.NORMAL,
                                                                  FeatureRequestStep.LOCAL_DELAYED);
        request.setFileUpdateMode(FeatureFileUpdateMode.REPLACE);
        return request;
    }

    private FeatureEntity buildFeatureEntity(FeatureFile... featureFiles) {
        Feature feature = initFeature();
        if (featureFiles != null) {
            for (FeatureFile featureFile : featureFiles) {
                feature.getFiles().add(featureFile);
            }
        }
        return FeatureEntity.build("sessionOwner", "session", feature, null, "model");
    }

    private Feature initFeature() {
        Feature feature = Feature.build("feature_id", "owner", null, null, EntityType.DATA, "model");
        feature.setUrn(getUrn(feature.getId()));
        return feature;
    }

    private FeatureUniformResourceName getUrn(String featureId) {
        UUID uuid = UUID.nameUUIDFromBytes(featureId.getBytes());
        return FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA, "tenant", uuid, 1);
    }

    private void simulateFileUpdate(FeatureUpdateRequest request,
                                    FeatureEntity entity,
                                    List<RequestResultInfoDto> results) {
        try {
            // Handle incoming request and send requests to STORAGE
            featureFilesService.handleFeatureUpdateFiles(request, entity);
            // Apply STORAGE response
            featureFilesService.updateFeatureLocations(entity,
                                                       results,
                                                       request.getFeature().getFiles());
        } catch (ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }
}