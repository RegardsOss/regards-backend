package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import static fr.cnes.regards.modules.indexer.dao.mapping.utils.GsonBetter.kv;
import static fr.cnes.regards.modules.indexer.dao.mapping.utils.GsonBetter.object;
import static fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping.RANGE_LOWER_BOUND;
import static fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping.RANGE_UPPER_BOUND;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import io.vavr.control.Option;

public class AttrDescToJsonMapping {

    public static final String ELASTICSEARCH_MAPPING_PROP_NAME = "ELASTICSEARCH_MAPPING";

    private static final Logger LOGGER = LoggerFactory.getLogger(AttrDescToJsonMapping.class);

    private static final JsonMerger MERGER = new JsonMerger();

    private final RangeAliasStrategy alias;

    public AttrDescToJsonMapping(RangeAliasStrategy alias) {
        this.alias = alias;
    }

    @VisibleForTesting
    static JsonObject nestedPropertiesStructure(String path, JsonObject propMapping) {
        String[] parts = path.split("\\.");
        ArrayUtils.reverse(parts);
        return Arrays.stream(parts).reduce(propMapping, (acc, part) -> object("properties", object(part, acc)),
                                           (a, b) -> merge(a, b));
    }

    private static JsonObject type(String type) {
        return object("type", type);
    }

    private static JsonObject merge(JsonObject... objs) {
        return MERGER.mergeAll(objs);
    }

    public static JsonObject stringMapping() {
        return object(kv("type", "text"), kv("fields", object("keyword", object(kv("type", "keyword")))));
    }

    public JsonObject toJsonMapping(AttributeDescription attrDescOrNull) {
        return Option.of(attrDescOrNull).flatMap(this::attemptConversion).getOrElse(JsonObject::new);
    }

    private Option<JsonObject> attemptConversion(AttributeDescription attrDesc) {
        return Option.of(attrDesc.getAttributeProperties()).map(ps -> Option.of(ps.get(ELASTICSEARCH_MAPPING_PROP_NAME))
                .map(mapping -> reuseExistingMappingProperty(attrDesc, mapping)).getOrElse(() -> dispatch(attrDesc)));
    }

    private JsonObject dispatch(AttributeDescription a) {
        switch (a.getType()) {
            case BOOLEAN:
                return toBooleanJsonMapping(a);
            case URL:
                return toURLJsonMapping(a);
            case STRING:
            case STRING_ARRAY:
                return toStringJsonMapping(a);
            case INTEGER:
            case INTEGER_ARRAY:
                return toIntegerJsonMapping(a);
            case INTEGER_INTERVAL:
                return toIntegerIntervalJsonMapping(a);
            case LONG:
            case LONG_ARRAY:
                return toLongJsonMapping(a);
            case LONG_INTERVAL:
                return toLongIntervalJsonMapping(a);
            case DOUBLE:
            case DOUBLE_ARRAY:
                return toDoubleJsonMapping(a);
            case DOUBLE_INTERVAL:
                return toDoubleIntervalJsonMapping(a);
            case DATE_ISO8601:
            case DATE_ARRAY:
                return toDateJsonMapping(a);
            case DATE_INTERVAL:
                return toDateIntervalJsonMapping(a);
            case OBJECT:
            case JSON:
                // FIXME : Handle mapping for json Objects thansk  to json schema
                return toObjectJsonMapping(a);
            default:
                throw new NotImplementedException("No mapping definition for property type " + a.getType());
        }
    }

    private JsonObject reuseExistingMappingProperty(AttributeDescription attrDesc, String mapping) {
        try {
            return nestedPropertiesStructure(attrDesc.getPath(), new JsonParser().parse(mapping).getAsJsonObject());
        } catch (IllegalStateException e) {
            LOGGER.warn("Impossible to parse declared {} property for attribute {}", ELASTICSEARCH_MAPPING_PROP_NAME,
                        attrDesc.getPath(), e);
            return new JsonObject();
        }
    }

    private JsonObject toURLJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("text"));
    }

    private JsonObject toBooleanJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("boolean"));
    }

    private JsonObject toObjectJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("object"));
    }

    private JsonObject toStringJsonMapping(AttributeDescription attrDesc) {
        switch (attrDesc.getRestriction()) {
            case DATE_ISO8601:
                return toDateJsonMapping(attrDesc);
            case GEOMETRY:
                return toGeometryJsonMapping(attrDesc);
            default:
                return nestedPropertiesStructure(attrDesc.getPath(), stringMapping());
        }
    }

    private JsonObject toGeometryJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("geo_shape"));
    }

    private JsonObject toIntegerJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("integer"));
    }

    private JsonObject toIntegerIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, type("integer"));
    }

    private JsonObject toLongJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("long"));
    }

    private JsonObject toLongIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, type("long"));
    }

    private JsonObject toDoubleJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(), type("double"));
    }

    private JsonObject toDoubleIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, type("double"));
    }

    private JsonObject toDateJsonMapping(AttributeDescription attrDesc) {
        return nestedPropertiesStructure(attrDesc.getPath(),
                                         object(kv("type", "date"), kv("format", "date_optional_time")));
    }

    private JsonObject toDateIntervalJsonMapping(AttributeDescription attrDesc) {
        return nestedSimpleRange(attrDesc, object(kv("type", "date"), kv("format", "date_optional_time")));
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
        },
        GTELTE {

            @Override
            JsonObject nestedSimpleRange(AttributeDescription attrDesc, JsonObject type) {
                String path = attrDesc.getPath();
                return merge(NO_ALIAS.nestedSimpleRange(attrDesc, type),
                             nestedPropertiesStructure(attrDesc.getPath() + ".gte",
                                                       object(kv("type", "alias"), kv("path", fullLowPath(path)))),
                             nestedPropertiesStructure(attrDesc.getPath() + ".lte",
                                                       object(kv("type", "alias"), kv("path", fullHighPath(path)))));
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
