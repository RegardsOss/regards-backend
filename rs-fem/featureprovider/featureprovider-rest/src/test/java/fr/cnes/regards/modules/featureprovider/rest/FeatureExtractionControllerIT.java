package fr.cnes.regards.modules.featureprovider.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureReferenceCollection;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_reference_rest", "regards.amqp.enabled=true",
                "spring.jpa.properties.hibernate.jdbc.batch_size=1024",
                "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class FeatureExtractionControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Test
    public void testCreateFeatureReferenceRequest() throws Exception {

        FeatureReferenceCollection collection = new FeatureReferenceCollection();
        collection.setMetadata(FeatureCreationSessionMetadata.build("owner",
                                                                    "session",
                                                                    PriorityLevel.NORMAL,
                                                                    false,
                                                                    StorageMetadata.build("id ")));
        collection.setFactory("PluginName");
        Set<JsonObject> parameters = new HashSet<>();
        JsonObject first = new JsonObject();
        parameters.add(first);
        collection.setParameters(parameters);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        documentFeatureReferenceCollectionRequestBody(requestBuilderCustomizer);

        performDefaultPost(FeatureExtractionController.EXTRACTION_PATH,
                           collection,
                           requestBuilderCustomizer,
                           "Something goes wrong during FeatureReferenceRequest creation")
                .andDo(MockMvcResultHandlers.print());

    }

    private void documentFeatureReferenceCollectionRequestBody(RequestBuilderCustomizer requestBuilderCustomizer) {
        ConstrainedFields fields = new ConstrainedFields(FeatureReferenceCollection.class);

        List<FieldDescriptor> lfd = new ArrayList<>();

        lfd.add(fields.withPath("factory", "Extraction plugin business ID"));
        lfd.add(fields.withPath("parameters", "Extraction plugin parameters"));

        lfd.add(fields.withPath("metadata.override", "If we want to override previous version"));
        lfd.add(fields.withPath("metadata.session", "The session name"));
        lfd.add(fields.withPath("metadata.sessionOwner", "The session owner"));
        lfd.add(fields.withPath("metadata.storages", "Target storages"));
        lfd.add(fields.withPath("metadata.storages[].pluginBusinessId", "Storage identifier"));

        requestBuilderCustomizer.document(PayloadDocumentation.relaxedRequestFields(Attributes.attributes(Attributes
                                                                                                                  .key(RequestBuilderCustomizer.PARAM_TITLE)
                                                                                                                  .value("Feature Collection manipulation")),
                                                                                    lfd.toArray(new FieldDescriptor[0])));
    }

}
