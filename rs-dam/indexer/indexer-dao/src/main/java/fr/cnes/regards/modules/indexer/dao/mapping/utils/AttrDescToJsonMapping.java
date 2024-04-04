package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import io.vavr.control.Option;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static fr.cnes.regards.modules.indexer.dao.mapping.utils.GsonBetter.kv;
import static fr.cnes.regards.modules.indexer.dao.mapping.utils.GsonBetter.object;
import static fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping.RANGE_LOWER_BOUND;
import static fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping.RANGE_UPPER_BOUND;

public class AttrDescToJsonMapping {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttrDescToJsonMapping.class);

    private static final String INDEX_KEY = "index";

    private static final String TYPE_KEY = "type";

    private static final String FORMAT_KEY = "format";

    private static final String PATH_KEY = "path";

    private static final String FIELDDATA_KEY = "fielddata";

    private static final String FIELDS_KEY = "fields";

    private static final String KEYWORD_KEY = "keyword";

    public static final String ELASTICSEARCH_MAPPING_PROP_NAME = "ELASTICSEARCH_MAPPING";

    private static final JsonMerger MERGER = new JsonMerger();

    private final RangeAliasStrategy alias;

    public AttrDescToJsonMapping(RangeAliasStrategy alias) {
        this.alias = alias;
    }

    @VisibleForTesting
    static JsonObject nestedPropertiesStructure(String path, JsonObject propMapping) {
        String[] parts = path.split("\\.");
        ArrayUtils.reverse(parts);
        return Arrays.stream(parts)
                     .reduce(propMapping,
                             (acc, part) -> object("properties", object(part, acc)),
                             (a, b) -> merge(a, b));
    }

    private static JsonObject type(String type, boolean indexed) {
        return object(kv(TYPE_KEY, type), kv(INDEX_KEY, indexed));
    }

    private static JsonObject merge(JsonObject... objs) {
        return MERGER.mergeAll(objs);
    }

    @Deprecated
    public static JsonObject stringMapping() {
        return stringMapping(true);
    }

    public static JsonObject stringMapping(boolean indexed) {
        return object(kv(TYPE_KEY, "text"),
                      kv(FIELDDATA_KEY, true),
                      kv(FIELDS_KEY, object(KEYWORD_KEY, object(kv(TYPE_KEY, KEYWORD_KEY), kv(INDEX_KEY, indexed)))));
    }

    public JsonObject toJsonMapping(AttributeDescription attrDescOrNull) {
        return Option.of(attrDescOrNull).flatMap(this::attemptConversion).getOrElse(JsonObject::new);
    }

    private Option<JsonObject> attemptConversion(AttributeDescription attrDesc) {
        return Option.of(attrDesc.getAttributeProperties())
                     .map(ps -> Option.of(ps.get(ELASTICSEARCH_MAPPING_PROP_NAME))
                                      .map(mapping -> reuseExistingMappingProperty(attrDesc, mapping))
                                      .getOrElse(() -> dispatch(attrDesc)));
    }

    @SuppressWarnings("java:S1541") // cyclomatic complexity to high
    private JsonObject dispatch(AttributeDescription a) {
        // If mapping is fixed by configuration return it
        if (a.getFixedMapping() != null) {
            return new JsonParser().parse(a.getFixedMapping()).getAsJsonObject();
        }
        // Else generate auto mapping
        return switch (a.getType()) {
            case BOOLEAN -> toBooleanJsonMapping(a);
            case URL -> toURLJsonMapping(a);
            case STRING, STRING_ARRAY -> toStringJsonMapping(a);
            case INTEGER, INTEGER_ARRAY -> toIntegerJsonMapping(a);
            case INTEGER_RANGE -> toIntegerRangeJsonMapping(a);
            case INTEGER_INTERVAL -> toIntegerIntervalJsonMapping(a);
            case LONG, LONG_ARRAY -> toLongJsonMapping(a);
            case LONG_RANGE -> toLongRangeJsonMapping(a);
            case LONG_INTERVAL -> toLongIntervalJsonMapping(a);
            case DOUBLE, DOUBLE_ARRAY -> toDoubleJsonMapping(a);
            case DOUBLE_RANGE -> toDoubleRangeJsonMapping(a);
            case DOUBLE_INTERVAL -> toDoubleIntervalJsonMapping(a);
            case DATE_ISO8601, DATE_ARRAY -> toDateJsonMapping(a);
            case DATE_RANGE -> toDateRangeJsonMapping(a);
            case DATE_INTERVAL -> toDateIntervalJsonMapping(a);
            case OBJECT, JSON -> toObjectJsonMapping(a);
            default -> throw new NotImplementedException("No mapping definition for property type " + a.getType());
        };
    }

    private JsonObject reuseExistingMappingProperty(AttributeDescription attrDesc, String mapping) {
        try {
            return nestedPropertiesStructure(attrDesc.getPath(), new JsonParser().parse(mapping).getAsJsonObject());
        } catch (IllegalStateException e) {
            LOGGER.warn("Impossible to parse declared {} property for attribute {}",
                        ELASTICSEARCH_MAPPING_PROP_NAME,
                        attrDesc.getPath(),
                        e);
            return new JsonObject();
        }
    }

    private JsonObject toURLJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), stringMapping(attrDesc.isIndexed()));
    }

    private JsonObject toBooleanJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("boolean", attrDesc.isIndexed()));
    }

    private JsonObject toObjectJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("object", attrDesc.isIndexed()));
    }

    private JsonObject toStringJsonMapping(AttributeDescription attrDesc) {
        switch (attrDesc.getRestriction()) {
            case DATE_ISO8601:
                return toDateJsonMapping(attrDesc);
            case GEOMETRY:
                return toGeometryJsonMapping(attrDesc);
            default:
                return nestedPropertiesStructure(attrDesc.getPath(), stringMapping(attrDesc.isIndexed()));
        }
    }

    private JsonObject toGeometryJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("geo_shape", attrDesc.isIndexed()));
    }

    private JsonObject toIntegerJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("integer", attrDesc.isIndexed()));
    }

    private JsonObject toIntegerRangeJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("integer_range", attrDesc.isIndexed()));
    }

    private JsonObject toIntegerIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, type("integer", attrDesc.isIndexed()));
    }

    private JsonObject toLongJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("long", attrDesc.isIndexed()));
    }

    private JsonObject toLongRangeJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("long_range", attrDesc.isIndexed()));
    }

    private JsonObject toLongIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, type("long", attrDesc.isIndexed()));
    }

    private JsonObject toDoubleJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("double", attrDesc.isIndexed()));
    }

    private JsonObject toDoubleRangeJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("double_range", attrDesc.isIndexed()));
    }

    private JsonObject toDoubleIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, type("double", attrDesc.isIndexed()));
    }

    private JsonObject toDateJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(),
                                         object(kv(TYPE_KEY, "date"),
                                                kv(INDEX_KEY, attrDesc.isIndexed()),
                                                kv(FORMAT_KEY, "date_optional_time")));
    }

    private JsonObject toDateRangeJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("date_range", attrDesc.isIndexed()));
    }

    private JsonObject toDateIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc,
                                 object(kv(TYPE_KEY, "date"),
                                        kv(INDEX_KEY, attrDesc.isIndexed()),
                                        kv(FORMAT_KEY, "date_optional_time")));
    }

    private JsonObject nestedSimpleRange(AttributeDescription attrDesc, JsonObject type) {
        return alias.nestedSimpleRange(attrDesc, type);
    }

    public enum RangeAliasStrategy {

        NO_ALIAS {
            @Override
            JsonObject nestedSimpleRange(AttributeDescription attrDesc, JsonObject type) {
                String path = attrDesc.getPath();
                return merge(nestedPropertiesStructure(fullLowPath(path), type),
                             nestedPropertiesStructure(fullHighPath(path), type));
            }
        }, GTELTE {
            @Override
            JsonObject nestedSimpleRange(AttributeDescription attrDesc, JsonObject type) {
                String path = attrDesc.getPath();
                return merge(NO_ALIAS.nestedSimpleRange(attrDesc, type),
                             nestedPropertiesStructure(attrDesc.getPath() + ".gte",
                                                       object(kv(TYPE_KEY, "alias"), kv(PATH_KEY, fullLowPath(path)))),
                             nestedPropertiesStructure(attrDesc.getPath() + ".lte",
                                                       object(kv(TYPE_KEY, "alias"),
                                                              kv(PATH_KEY, fullHighPath(path)))));
            }
        };

        String fullLowPath(String path) {
            return path + "." + RANGE_LOWER_BOUND;
        }

        String fullHighPath(String path) {
            return path + "." + RANGE_UPPER_BOUND;
        }

        abstract JsonObject nestedSimpleRange(AttributeDescription attrDesc, JsonObject type);
    }

}
