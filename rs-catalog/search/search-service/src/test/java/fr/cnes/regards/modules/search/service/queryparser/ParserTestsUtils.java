/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.queryparser;

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
public class ParserTestsUtils {

    public static final String BOOLEAN_FIELD = "isTrue";

    public static final String INTEGER_FIELD = "altitude";

    public static final String INTEGER_RANGE_FIELD = "age";

    public static final String DOUBLE_FIELD = "bpm";

    public static final String DOUBLE_RANGE_FIELD = "height";

    public static final String LONG_FIELD = "speed";

    public static final String LONG_RANGE_FIELD = "distance";

    public static final String STRING_FIELD = "title";

    public static final String STRING_FIELD_1 = "author";

    public static final String STRING_RANGE_FIELD = "movie";

    public static final String LOCAL_DATE_TIME_FIELD = "date";

    public static final String LOCAL_DATE_TIME_RANGE_FIELD = "arrival";

    public static final String INTEGER_ARRAY_FIELD = "years";

    public static final String DOUBLE_ARRAY_FIELD = "duration";

    public static final String LONG_ARRAY_FIELD = "boxoffice";

    public static final String STRING_ARRAY_FIELD = "cast";

    public static final String LOCAL_DATE_TIME_ARRAY = "releases";

    // Build some attribute models for all attribute types
    public static final AttributeModel BOOLEAN_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(BOOLEAN_FIELD, AttributeType.BOOLEAN).get();

    public static final AttributeModel INTEGER_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(INTEGER_FIELD, AttributeType.INTEGER).get();

    public static final AttributeModel DOUBLE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(DOUBLE_FIELD, AttributeType.DOUBLE).get();

    public static final AttributeModel LONG_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LONG_FIELD, AttributeType.LONG).get();

    public static final AttributeModel STRING_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(STRING_FIELD, AttributeType.STRING).get();

    public static final AttributeModel STRING_ATTRIBUTE_MODEL_1 = AttributeModelBuilder
            .build(STRING_FIELD_1, AttributeType.STRING).get();

    public static final AttributeModel LOCAL_DATE_TIME_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LOCAL_DATE_TIME_FIELD, AttributeType.DATE_ISO8601).get();

    public static final AttributeModel INTEGER_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(INTEGER_RANGE_FIELD, AttributeType.INTEGER_INTERVAL).get();

    public static final AttributeModel DOUBLE_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(DOUBLE_RANGE_FIELD, AttributeType.DOUBLE_INTERVAL).get();

    public static final AttributeModel LONG_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LONG_RANGE_FIELD, AttributeType.LONG_INTERVAL).get();

    public static final AttributeModel LOCAL_DATE_TIME_RANGE_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LOCAL_DATE_TIME_RANGE_FIELD, AttributeType.DATE_INTERVAL).get();

    public static final AttributeModel INTEGER_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(INTEGER_ARRAY_FIELD, AttributeType.INTEGER_ARRAY).get();

    public static final AttributeModel DOUBLE_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(DOUBLE_ARRAY_FIELD, AttributeType.DOUBLE_ARRAY).get();

    public static final AttributeModel LONG_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LONG_ARRAY_FIELD, AttributeType.LONG_ARRAY).get();

    public static final AttributeModel STRING_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(STRING_ARRAY_FIELD, AttributeType.STRING_ARRAY).get();

    public static final AttributeModel LOCAL_DATE_TIME_ARRAY_ATTRIBUTE_MODEL = AttributeModelBuilder
            .build(LOCAL_DATE_TIME_ARRAY, AttributeType.DATE_ARRAY).get();

    public static final List<AttributeModel> LIST = Lists
            .newArrayList(BOOLEAN_ATTRIBUTE_MODEL, INTEGER_ATTRIBUTE_MODEL, DOUBLE_ATTRIBUTE_MODEL,
                          LONG_ATTRIBUTE_MODEL, STRING_ATTRIBUTE_MODEL, STRING_ATTRIBUTE_MODEL_1,
                          LOCAL_DATE_TIME_ATTRIBUTE_MODEL, INTEGER_RANGE_ATTRIBUTE_MODEL, DOUBLE_RANGE_ATTRIBUTE_MODEL,
                          LONG_RANGE_ATTRIBUTE_MODEL, LOCAL_DATE_TIME_RANGE_ATTRIBUTE_MODEL,
                          INTEGER_ARRAY_ATTRIBUTE_MODEL, DOUBLE_ARRAY_ATTRIBUTE_MODEL, LONG_ARRAY_ATTRIBUTE_MODEL,
                          STRING_ARRAY_ATTRIBUTE_MODEL, LOCAL_DATE_TIME_ARRAY_ATTRIBUTE_MODEL);

    public static final ResponseEntity<List<Resource<AttributeModel>>> CLIENT_RESPONSE = ResponseEntity
            .ok(HateoasUtils.wrapList(ParserTestsUtils.LIST));

}
