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
package fr.cnes.regards.modules.feature.rest;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.*;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;
import fr.cnes.regards.modules.model.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.model.service.exception.ImportException;
import fr.cnes.regards.modules.model.service.xml.IComputationPluginService;
import fr.cnes.regards.modules.model.service.xml.XmlImportHelper;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Sébastien Binda
 */
public abstract class AbstractFeatureIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureIT.class);

    @Autowired
    private IComputationPluginService cps;

    @Autowired
    protected IModelClient modelClientMock;

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory factory;

    @Autowired
    private IModelAttrAssocClient modelAttrAssocClientMock;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IFeatureEntityRepository featureRepo;

    @Autowired
    protected IFeatureCreationRequestRepository featureCreationRequestRepo;

    @Autowired
    protected IFeatureUpdateRequestRepository featureUpdateRequestRepo;

    @Autowired
    protected IFeatureDeletionRequestRepository featureDeletionRequestRepo;

    @Autowired
    protected IFeatureNotificationRequestRepository notificationRequestRepo;

    @Autowired
    protected IFeatureSaveMetadataRequestRepository featureSaveMetadataRequestRepository;

    @Autowired
    protected IFeatureDisseminationInfoRepository featureDisseminationInfoRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IFeatureCreationService featureService;

    @Before
    public void init() {
        this.featureCreationRequestRepo.deleteAllInBatch();
        this.featureUpdateRequestRepo.deleteAllInBatch();
        this.featureDeletionRequestRepo.deleteAllInBatch();
        this.featureSaveMetadataRequestRepository.deleteAllInBatch();
        this.featureDisseminationInfoRepository.deleteAllInBatch();
        this.featureRepo.deleteAllInBatch();
        this.notificationRequestRepo.deleteAllInBatch();
        this.jobInfoRepository.deleteAll();
    }

    public void createFeatures(String providerId, int nbFeatures, String source, String session) {
        OffsetDateTime start = OffsetDateTime.now();
        for (int i = 0; i < nbFeatures; i++) {
            Feature featureToAdd = initValidFeature(providerId + i);
            List<FeatureCreationRequestEvent> features = Lists.newArrayList();
            FeatureCreationSessionMetadata meta = FeatureCreationSessionMetadata.build(source,
                                                                                       session,
                                                                                       PriorityLevel.NORMAL,
                                                                                       Lists.newArrayList(
                                                                                           StorageMetadata.build("disk")),
                                                                                       true,
                                                                                       false);
            features.add(FeatureCreationRequestEvent.build("owner", meta, featureToAdd));
            Assert.assertEquals(1, featureService.registerRequests(features).getGranted().size());
        }
        featureService.scheduleRequests();
        waitFeature(nbFeatures, start, 10000, getDefaultTenant());
    }

    protected FeatureDisseminationInfo createFeatureDisseminationInfo(FeatureUniformResourceName productUrn,
                                                                      String recipientLabel,
                                                                      boolean ackRequired) {
        return new FeatureDisseminationInfo(new FeatureUpdateDisseminationRequest(productUrn,
                                                                                  recipientLabel,
                                                                                  FeatureUpdateDisseminationInfoType.PUT,
                                                                                  OffsetDateTime.now(),
                                                                                  ackRequired,
                                                                                  false));

    }

    /**
     * Mock model client importing model specified by its filename
     *
     * @param filename model filename found using {@link Class#getResourceAsStream(String)}
     * @return mocked model name
     */
    public String mockModelClient(String filename,
                                  IComputationPluginService cps,
                                  MultitenantFlattenedAttributeAdapterFactory factory,
                                  String tenant,
                                  IModelAttrAssocClient modelAttrAssocClientMock) {

        try (InputStream input = FeatureControllerIT.class.getResourceAsStream(filename)) {
            // Import model
            Iterable<ModelAttrAssoc> assocs = XmlImportHelper.importModel(input, cps);

            // Translate to resources and attribute models and extract model name
            String modelName = null;
            List<AttributeModel> atts = new ArrayList<>();
            List<EntityModel<ModelAttrAssoc>> resources = new ArrayList<>();
            for (ModelAttrAssoc assoc : assocs) {
                atts.add(assoc.getAttribute());
                resources.add(EntityModel.of((assoc)));
                if (modelName == null) {
                    modelName = assoc.getModel().getName();
                }
            }

            // Property factory registration
            factory.registerAttributes(tenant, atts);

            // Mock client
            List<EntityModel<Model>> models = new ArrayList<EntityModel<Model>>();
            Model mockModel = Mockito.mock(Model.class);
            Mockito.when(mockModel.getName()).thenReturn(modelName);
            models.add(EntityModel.of((mockModel)));
            Mockito.when(modelClientMock.getModels(null)).thenReturn(ResponseEntity.ok(models));
            Mockito.when(modelAttrAssocClientMock.getModelAttrAssocs(modelName))
                   .thenReturn(ResponseEntity.ok(resources));

            return modelName;
        } catch (IOException | ImportException e) {
            String errorMessage = "Cannot import model";
            LOGGER.debug(errorMessage);
            throw new AssertionError(errorMessage);
        }
    }

    protected Feature initValidFeature(String providerId) {
        String model = mockModelClient("feature_model_01.xml",
                                       cps,
                                       factory,
                                       this.getDefaultTenant(),
                                       modelAttrAssocClientMock);

        Feature feature = Feature.build(providerId,
                                        "owner",
                                        null,
                                        IGeometry.point(IGeometry.position(10.0, 20.0)),
                                        EntityType.DATA,
                                        model);
        feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
        feature.addProperty(IProperty.buildObject("file_characterization", IProperty.buildBoolean("valid", true)));
        feature.withFiles(FeatureFile.build(FeatureFileAttributes.build(DataType.RAWDATA,
                                                                        MimeType.valueOf("application/xml"),
                                                                        "filename",
                                                                        100L,
                                                                        "MD5",
                                                                        "checksum"),
                                            FeatureFileLocation.build("http://www.test.com/filename.xml")));

        return feature;
    }

    protected Feature initValidUpdateFeature() {
        String model = mockModelClient("feature_model_01.xml",
                                       cps,
                                       factory,
                                       this.getDefaultTenant(),
                                       modelAttrAssocClientMock);

        Feature feature = Feature.build("MyId",
                                        "owner",
                                        FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                                         EntityType.DATA,
                                                                         "tenant",
                                                                         UUID.randomUUID(),
                                                                         1),
                                        null,
                                        EntityType.DATA,
                                        model);
        feature.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", false),
                                                  IProperty.buildDate("invalidation_date", OffsetDateTime.now())));
        return feature;
    }

    /**
     * Wait until feature are properly created
     *
     * @param expected expected feature number
     * @param from     feature updated after from date. May be <code>null</code>.
     * @param timeout  timeout in milliseconds
     */
    protected void waitFeature(long expected, @Nullable OffsetDateTime from, long timeout, String tenant) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            if (from != null) {
                runtimeTenantResolver.forceTenant(tenant);
                return featureRepo.countByLastUpdateGreaterThan(from) == expected;
            }
            return featureRepo.count() == expected;
        });
    }

}
