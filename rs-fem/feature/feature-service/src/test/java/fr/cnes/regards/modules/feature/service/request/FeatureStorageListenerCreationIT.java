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
package fr.cnes.regards.modules.feature.service.request;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureCopyRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author kevin
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_listener_creation",
                                   "regards.amqp.enabled=true",
                                   "spring.task.scheduling.pool.size=2",
                                   "regards.feature.metrics.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles({ "testAmqp" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS,
                hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public class FeatureStorageListenerCreationIT extends AbstractFeatureMultitenantServiceIT {

    @Autowired
    private IFeatureCreationRequestRepository fcrRepo;

    @Autowired
    private IFeatureCopyRequestRepository featureCopyRepo;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private FeatureStorageListener listener;

    private boolean isToNotify;

    @Override
    @Before
    public void before() throws Exception {
        this.featureCopyRepo.deleteAll();
        super.before();
        this.isToNotify = initDefaultNotificationSettings();
    }

    @Test
    public void testHandlerStorageOk() {

        initData(1);

        assertEquals(1, fcrRepo.count());

        mockStorageHelper.mockFeatureCreationStorageSuccess();
        if (this.isToNotify) {
            mockNotificationSuccess();
        }
        // the FeatureCreationRequest must be deleted
        assertEquals(0, fcrRepo.count());
        // the FeatureEntity must remain
        assertEquals(1, featureRepo.count());

    }

    @Test
    public void testHandlerStorageError() {

        RequestInfo info = RequestInfo.build();
        initData(info);
        FileReferenceDto ref = null;
        info.getErrorRequests()
            .add(RequestResultInfoDto.build(info.getGroupId(),
                                            RandomChecksumUtils.generateRandomChecksum(),
                                            "",
                                            "",
                                            Lists.newArrayList(),
                                            ref,
                                            "Simulated error"));

        this.listener.onStoreError(Sets.newHashSet(info));

        // the FeatureCreationRequest must remain
        assertEquals(1, fcrRepo.count());
        // it state must be an error
        assertEquals(RequestState.ERROR, fcrRepo.findAll().get(0).getState());
        // the FeatureEntity must remain
        assertEquals(1, featureRepo.count());

    }

    @Test
    public void testHandlerReferenceOk() {

        initData(1);

        assertEquals(1, fcrRepo.count());

        mockStorageHelper.mockFeatureCreationStorageSuccess();
        if (this.isToNotify) {
            mockNotificationSuccess();
        }

        // the FeatureCreationRequest must be deleted
        assertEquals(0, fcrRepo.count());
        // the FeatureEntity must remain
        assertEquals(1, featureRepo.count());

    }

    @Test
    public void testHandlerReferenceError() {

        RequestInfo info = RequestInfo.build();

        initData(info);
        FileReferenceDto ref = null;
        info.getErrorRequests()
            .add(RequestResultInfoDto.build(info.getGroupId(),
                                            RandomChecksumUtils.generateRandomChecksum(),
                                            "",
                                            "",
                                            Lists.newArrayList(),
                                            ref,
                                            "Simulated error"));

        this.listener.onReferenceError(Sets.newHashSet(info));

        // the FeatureCreationRequest must remain
        assertEquals(1, fcrRepo.count());
        // it state must be an error
        assertEquals(RequestState.ERROR, fcrRepo.findAll().get(0).getState());
        // the FeatureEntity must remain
        assertEquals(1, featureRepo.count());

    }

    @Test
    public void testOnCopyOk() {

        RequestInfo info = RequestInfo.build();

        String checksum = initData(info);
        RequestResultInfoDto resultInfo = RequestResultInfoDto.build(null,
                                                                     checksum,
                                                                     null,
                                                                     "dtc",
                                                                     Sets.newHashSet(featureRepo.findAll()
                                                                                                .get(0)
                                                                                                .getFeature()
                                                                                                .getUrn()
                                                                                                .toString()),
                                                                     new FileReferenceDto(),
                                                                     null);

        info.getSuccessRequests().add(resultInfo);
        listener.onCopySuccess(Sets.newHashSet(info));
        // we should have 1 copy request in database
        assertEquals(1, this.featureCopyRepo.count());
        // wait that this request is treated
        waitRequest(this.featureCopyRepo, 0, 60000);

        // we expect have dtc in FileLocation
        assertEquals(3, featureRepo.findAll().get(0).getFeature().getFiles().get(0).getLocations().size());
        assertTrue(featureRepo.findAll()
                              .get(0)
                              .getFeature()
                              .getFiles()
                              .get(0)
                              .getLocations()
                              .stream()
                              .anyMatch(loc -> loc.getUrl().equals("dtc")));

    }

    @Test
    public void testOnCopyFailWithNonExistingChecksum() throws InterruptedException {

        RequestInfo info = RequestInfo.build();

        initData(info);
        RequestResultInfoDto resultInfo = RequestResultInfoDto.build(null,
                                                                     RandomChecksumUtils.generateRandomChecksum(),
                                                                     null,
                                                                     "dtc",
                                                                     Sets.newHashSet(featureRepo.findAll()
                                                                                                .get(0)
                                                                                                .getFeature()
                                                                                                .getUrn()
                                                                                                .toString()),
                                                                     new FileReferenceDto(),
                                                                     null);

        info.getSuccessRequests().add(resultInfo);
        this.listener.onCopySuccess(Sets.newHashSet(info));
        this.waitForErrorState(featureCopyRepo);

        // we expect have always 2 FileLocation
        assertEquals(2, featureRepo.findAll().get(0).getFeature().getFiles().get(0).getLocations().size());

    }

    @Test
    public void testOnCopyFailWithNonExistingUrn() throws InterruptedException {

        RequestInfo info = RequestInfo.build();

        initData(info);
        RequestResultInfoDto resultInfo = RequestResultInfoDto.build(null,
                                                                     RandomChecksumUtils.generateRandomChecksum(),
                                                                     null,
                                                                     "dtc",
                                                                     Sets.newHashSet(FeatureUniformResourceName.build(
                                                                         FeatureIdentifier.FEATURE,
                                                                         EntityType.DATA,
                                                                         "fail",
                                                                         UUID.randomUUID(),
                                                                         1).toString()),
                                                                     new FileReferenceDto(),
                                                                     null);

        info.getSuccessRequests().add(resultInfo);
        this.listener.onCopySuccess(Sets.newHashSet(info));
        this.waitForErrorState(featureCopyRepo);

        // we expect have always 2 FileLocation
        assertEquals(2, featureRepo.findAll().get(0).getFeature().getFiles().get(0).getLocations().size());

    }

    private String initData(RequestInfo info) {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String model = "model";
        FeatureCreationRequest fcr = FeatureCreationRequest.build("id1",
                                                                  "owner",
                                                                  OffsetDateTime.now(),
                                                                  RequestState.GRANTED,
                                                                  new HashSet<String>(),
                                                                  Feature.build("id1",
                                                                                "test",
                                                                                FeatureUniformResourceName.build(
                                                                                    FeatureIdentifier.FEATURE,
                                                                                    EntityType.DATA,
                                                                                    "lol",
                                                                                    UUID.randomUUID(),
                                                                                    1),
                                                                                IGeometry.point(IGeometry.position(10.0,
                                                                                                                   20.0)),
                                                                                EntityType.DATA,
                                                                                model),
                                                                  FeatureCreationMetadataEntity.build("owner",
                                                                                                      "session",
                                                                                                      Lists.emptyList(),
                                                                                                      true),
                                                                  FeatureRequestStep.LOCAL_SCHEDULED,
                                                                  PriorityLevel.NORMAL);
        fcr.setGroupId(info.getGroupId());

        FeatureEntity feature = FeatureEntity.build("owner",
                                                    "session",
                                                    Feature.build("id2",
                                                                  "test",
                                                                  FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                                                   EntityType.DATA,
                                                                                                   "peps",
                                                                                                   UUID.randomUUID(),
                                                                                                   1),
                                                                  IGeometry.point(IGeometry.position(10.0, 20.0)),
                                                                  EntityType.DATA,
                                                                  model).withHistory("test"),
                                                    null,
                                                    model);
        List<FeatureFile> filles = new ArrayList<>();
        filles.add(FeatureFile.build(FeatureFileAttributes.build(DataType.DESCRIPTION,
                                                                 new MimeType("mime"),
                                                                 "toto",
                                                                 1024l,
                                                                 "MD5",
                                                                 checksum),
                                     FeatureFileLocation.build("www.google.com", "GPFS"),
                                     FeatureFileLocation.build("www.perdu.com", "GPFS")));
        feature.getFeature().setFiles(filles);
        this.featureRepo.save(feature);
        fcr.setFeatureEntity(feature);
        this.fcrRepo.save(fcr);
        return checksum;
    }
}
