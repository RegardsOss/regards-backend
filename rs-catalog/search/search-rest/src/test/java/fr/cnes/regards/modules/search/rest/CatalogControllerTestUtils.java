/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public class CatalogControllerTestUtils {

    /**
     * Dummy OpenSearch request
     */
    // public static final String Q = "integer:(2 AND 3) OR string:hello";
    public static final String Q = "label:mycollection";

    /**
     * OpenSearch request expected to find a collection
     */
    public static final String Q_FINDS_ONE_COLLECTION = "label:mycollection";

    /**
     * OpenSearch request expected to find two datasets
     */
    public static final String Q_FINDS_TWO_DATASETS = "label:mydataset";

    /**
     * OpenSearch request expected to find one dataobject
     */
    public static final String Q_FINDS_ONE_DATAOBJECT = "label:mydataobject";

    /**
     * OpenSearch request expected to find one document
     */
    public static final String Q_FINDS_ONE_DOCUMENT = "label:mydocument";

    /**
     * A dummy list of facets
     */
    public static final List<String> FACETS = Lists.newArrayList("integer", "label", "date");

    /**
     * The dummy list of facets as array
     */
    public static final String[] FACETS_AS_ARRAY = FACETS.toArray(new String[FACETS.size()]);

    private static final AttributeModel INTEGER_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build("integer", AttributeType.INTEGER).get();

    private static final AttributeModel STRING_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build("label", AttributeType.STRING).get();

    private static final AttributeModel DATE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build("date", AttributeType.DATE_ISO8601).get();

    private static final List<AttributeModel> LIST = Lists.newArrayList(INTEGER_ATTRIBUTE_MODEL, STRING_ATTRIBUTE_MODEL,
                                                                        DATE_ATTRIBUTE_MODEL);

    public static final ResponseEntity<List<Resource<AttributeModel>>> CLIENT_RESPONSE = ResponseEntity
            .ok(HateoasUtils.wrapList(LIST));

}
