package fr.cnes.regards.modules.feature.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.validation.MapBindingResult;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureDeletionCollection;
import fr.cnes.regards.modules.feature.dto.FeatureMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureReferenceCollection;
import fr.cnes.regards.modules.feature.dto.FeatureSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureUpdateCollection;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureReferenceRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureValidationService;
import fr.cnes.regards.modules.model.client.IModelClient;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature",
        "regards.amqp.enabled=true", "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
        "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@ContextConfiguration(classes = { AbstractMultitenantServiceTest.ScanningConfiguration.class })
public class FeatureControllerIT extends AbstractFeatureIT {

    @Autowired
    private IFeatureValidationService validationMock;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IModelClient modelClientMock;

    private static final String FEATURE_CREATION_REQUEST_ERROR = "Something goes wrong during FeatureCreationRequest creation";

    private static final String FEATURE_UPDATE_REQUEST_ERROR = "Something goes wrong during FeatureUpdateRequest creation";

    @Test
    public void testCreateValidFeatureCreationRequest() throws Exception {

        Feature featureToAdd = initValidFeature();
        FeatureUpdateCollection collection = new FeatureUpdateCollection();
        collection.add(featureToAdd);
        collection.setRequestOwner("test");
        List<StorageMetadata> metadata = new ArrayList<StorageMetadata>();
        metadata.add(StorageMetadata.build("disk"));
        // we will mock validation plugin and consider the feature is valid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any()))
                .thenReturn(new MapBindingResult(new HashMap<>(), Feature.class.getName()));
        collection.setMetadata(FeatureSessionMetadata.build("owner", "session", PriorityLevel.NORMAL, metadata));

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);

        documentFeatureCollectionRequestBody(requestBuilderCustomizer, false);

        performDefaultPost(FeatureController.PATH_FEATURES, collection, requestBuilderCustomizer,
                           FEATURE_CREATION_REQUEST_ERROR).andDo(MockMvcResultHandlers.print());

    }

    @Test
    public void testCreateUnValidFeatureCreationRequest() throws Exception {
        Feature featureToAdd;
        featureToAdd = new Feature();
        featureToAdd.setEntityType(EntityType.DATA);
        featureToAdd.setGeometry(IGeometry.point(IGeometry.position(10.0, 20.0)));
        featureToAdd.setUrn(null);
        featureToAdd.setModel("model");

        FeatureUpdateCollection collection = new FeatureUpdateCollection();
        collection.add(featureToAdd);
        List<StorageMetadata> metadata = new ArrayList<StorageMetadata>();
        metadata.add(StorageMetadata.build("disk"));

        collection.setMetadata(FeatureSessionMetadata.build("owner", "session", PriorityLevel.NORMAL, metadata));
        collection.setRequestOwner("test");
        MapBindingResult errors = new MapBindingResult(new HashMap<>(), Feature.class.getName());
        errors.reject("error code");
        // we will mock validation plugin and consider the feature is unvalid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any())).thenReturn(errors);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);

        performDefaultPost(FeatureController.PATH_FEATURES, collection, requestBuilderCustomizer,
                           FEATURE_CREATION_REQUEST_ERROR).andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testCreateFeatureReferenceRequest() throws Exception {

        FeatureReferenceCollection collection = new FeatureReferenceCollection();
        collection.setMetadata(FeatureCreationSessionMetadata.build("owner", "session", PriorityLevel.NORMAL, false,
                                                                    StorageMetadata.build("id ")));
        collection.setFactory("PluginName");
        Set<JsonObject> parameters = new HashSet<>();
        JsonObject first = new JsonObject();
        parameters.add(first);
        collection.setParameters(parameters);

        // we will mock validation plugin and consider the feature is valid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any()))
                .thenReturn(new MapBindingResult(new HashMap<>(), FeatureReferenceRequestEvent.class.getName()));

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        documentFeatureReferenceCollectionRequestBody(requestBuilderCustomizer);

        performDefaultPost(FeatureController.PATH_FEATURES + FeatureController.REFERENCE_PATH, collection,
                           requestBuilderCustomizer, FEATURE_CREATION_REQUEST_ERROR)
                                   .andDo(MockMvcResultHandlers.print());

    }

    @Test
    public void testCreateValidFeatureUpdateRequest() throws Exception {
        Feature feature = initValidUpdateFeature();
        FeatureUpdateCollection collection = new FeatureUpdateCollection();
        collection.add(feature);
        List<StorageMetadata> metadata = new ArrayList<StorageMetadata>();
        //        metadata.add(StorageMetadata.build("disk"));

        collection.setMetadata(FeatureMetadata.build(PriorityLevel.NORMAL, metadata));
        collection.setRequestOwner("test");
        // we will mock validation plugin and consider the feature is valid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any()))
                .thenReturn(new MapBindingResult(new HashMap<>(), Feature.class.getName()));
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        documentFeatureCollectionRequestBody(requestBuilderCustomizer, true);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        performDefaultPatch(FeatureController.PATH_FEATURES, collection, requestBuilderCustomizer,
                            FEATURE_UPDATE_REQUEST_ERROR).andDo(MockMvcResultHandlers.print());
    }

    @Test
    public void testCreateUnValidFeatureUpdateRequest() throws Exception {
        Feature feature;
        feature = new Feature();
        feature.setEntityType(EntityType.DATA);
        feature.setModel("model");
        feature.setGeometry(IGeometry.point(IGeometry.position(10.0, 20.0)));
        feature.setUrn(null);
        feature.setId("MyId");
        feature.setUrn(FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA, "tenant",
                                                        UUID.randomUUID(), 1));
        FeatureUpdateCollection collection = new FeatureUpdateCollection();
        collection.add(feature);
        List<StorageMetadata> metadata = new ArrayList<StorageMetadata>();
        metadata.add(StorageMetadata.build("disk"));

        collection.setMetadata(FeatureMetadata.build(PriorityLevel.NORMAL, metadata));
        collection.setRequestOwner("test");
        // we will mock validation plugin and consider the feature is valid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any()))
                .thenReturn(new MapBindingResult(new HashMap<>(), Feature.class.getName()));
        MapBindingResult errors = new MapBindingResult(new HashMap<>(), Feature.class.getName());
        errors.reject("error code");
        // we will mock validation plugin and consider the feature is unvalid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any())).thenReturn(errors);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);

        performDefaultPatch(FeatureController.PATH_FEATURES, collection, requestBuilderCustomizer,
                            FEATURE_CREATION_REQUEST_ERROR).andDo(MockMvcResultHandlers.print());

    }

    @Test
    public void testCreateFeatureDeletionRequest() throws Exception {

        FeatureDeletionCollection collection = new FeatureDeletionCollection();
        collection.addAll(Sets.newSet(FeatureUniformResourceName.build(FeatureIdentifier.FEATURE, EntityType.DATA,
                                                                       "tenant", UUID.randomUUID(), 1)));
        collection.setPriority(PriorityLevel.HIGH);

        MapBindingResult errors = new MapBindingResult(new HashMap<>(), FeatureDeletionRequest.class.getName());
        // we will mock validation plugin and consider the feature is unvalid
        Mockito.when(validationMock.validate(Mockito.any(), Mockito.any())).thenReturn(errors);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        documentFeatureDeletionCollectionRequestBody(requestBuilderCustomizer);

        performDefaultDelete(FeatureController.PATH_FEATURES, collection, requestBuilderCustomizer,
                             FEATURE_CREATION_REQUEST_ERROR).andDo(MockMvcResultHandlers.print());
    }

    private void documentFeatureCollectionRequestBody(RequestBuilderCustomizer requestBuilderCustomizer,
            boolean isUpdate) {
        ConstrainedFields fields = new ConstrainedFields(FeatureUpdateCollection.class);

        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        if (!isUpdate) {
            lfd.add(fields.withPath("metadata.storages", "Target storages"));
            lfd.add(fields.withPath("metadata.storages[].pluginBusinessId", "Storage identifier"));
        }
        //        lfd.add(fields.withPath("metadata.storages[].targetTypes",
        //                                "List of data object types accepted by this storage location (when storing AIPs)"));
        lfd.add(fields.withPath("features[].entityType", "Entity Type"));
        if (isUpdate) {
            lfd.add(fields.withPath("features[].urn",
                                    "Unique feature identifer based on provider identifier with versionning"));
        }
        if (!isUpdate) {
            lfd.add(fields.withPath("metadata.session", "The session name"));
            lfd.add(fields.withPath("metadata.sessionOwner", "The session owner"));
        }
        lfd.add(fields.withPath("features[].model", "Model"));
        lfd.add(fields.withPath("features[].id", "Technical id"));
        if (!isUpdate) {
            lfd.add(fields.withPath("features[].geometry", "GeoJson Coordinates"));
        }
        lfd.add(fields.withPath("features[].properties", "Properties"));
        if (!isUpdate) {
            lfd.add(fields.withPath("features[].files[].locations[].storage", "Storage"));
            lfd.add(fields.withPath("features[].files[].locations[].url", "Url location"));
            lfd.add(fields.withPath("features[].files[].attributes.dataType", "Data type"));
            lfd.add(fields.withPath("features[].files[].attributes.mimeType", "Media type"));
            lfd.add(fields.withPath("features[].files[].attributes.filename", "File name"));
            lfd.add(fields.withPath("features[].files[].attributes.filesize", "File size"));
            lfd.add(fields.withPath("features[].files[].attributes.algorithm", "Algorith for checksum computation"));
            lfd.add(fields.withPath("features[].files[].attributes.checksum", "Checksum"));
        }

        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Feature Collection manipulation")), lfd.toArray(new FieldDescriptor[lfd.size()])));
    }

    private void documentFeatureReferenceCollectionRequestBody(RequestBuilderCustomizer requestBuilderCustomizer) {
        ConstrainedFields fields = new ConstrainedFields(FeatureReferenceCollection.class);

        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        lfd.add(fields.withPath("factory", "Extraction plugin business ID"));
        lfd.add(fields.withPath("parameters", "Extraction plugin parameters"));

        lfd.add(fields.withPath("metadata.override", "If we want to override previous version"));
        lfd.add(fields.withPath("metadata.session", "The session name"));
        lfd.add(fields.withPath("metadata.sessionOwner", "The session owner"));
        lfd.add(fields.withPath("metadata.storages", "Target storages"));
        lfd.add(fields.withPath("metadata.storages[].pluginBusinessId", "Storage identifier"));

        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Feature Collection manipulation")), lfd.toArray(new FieldDescriptor[lfd.size()])));
    }

    private void documentFeatureDeletionCollectionRequestBody(RequestBuilderCustomizer requestBuilderCustomizer) {
        ConstrainedFields fields = new ConstrainedFields(FeatureDeletionCollection.class);

        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();

        lfd.add(fields.withPath("featuresUrns", "List of urns to delete"));
        lfd.add(fields.withPath("priority", "Priotity of the request"));

        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Feature Collection manipulation")), lfd.toArray(new FieldDescriptor[lfd.size()])));
    }
}
