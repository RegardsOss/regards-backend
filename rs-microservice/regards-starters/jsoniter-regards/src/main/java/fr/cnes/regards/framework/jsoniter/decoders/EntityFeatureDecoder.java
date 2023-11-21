package fr.cnes.regards.framework.jsoniter.decoders;

import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jsoniter.any.Any;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jsoniter.property.AttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.dto.properties.adapter.RangeMapping;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping.RANGE_LOWER_BOUND;
import static fr.cnes.regards.modules.model.dto.properties.adapter.IntervalMapping.RANGE_UPPER_BOUND;

public interface EntityFeatureDecoder extends SmartDecoder {

    Logger LOGGER = LoggerFactory.getLogger(EntityFeatureDecoder.class);

    AttributeModelPropertyTypeFinder getPropTypeFinder();

    Gson getGson();

    default void readTagsFilesProperties(Any feature, EntityFeature result) {
        feature.get("tags").asList().forEach(tag -> result.addTag(tag.toString()));

        feature.get("files").asMap().forEach((type, files) -> {
            DataType dataType = DataType.valueOf(type);
            files.asList().forEach(file -> result.getFiles().put(dataType, file.as(DataFile.class)));
        });

        feature.get("properties")
               .asMap()
               .forEach((name, value) -> getPropTypeFinder().getPropertyTypeForAttributeWithName(name)
                                                            .flatMap(type -> readProperty(name, value, type))
                                                            .peek(result::addProperty));

    }

    default void readGeometries(Any feature, EntityFeature result) {
        whenPresent(feature.get("geometry"), IGeometry.class, result::setGeometry);
        whenPresent(feature.get("normalizedGeometry"), IGeometry.class, result::setNormalizedGeometry);
    }

    default void readBasicFields(Any feature, EntityFeature result) {
        result.setVirtualId();
        result.setModel(stringOrNull(feature, "model"));
        result.setLast(feature.toBoolean("last"));
        result.setVersion(feature.toInt("version"));
        result.setCrs(stringOrNull(feature, "crs"));
    }

    default Option<IProperty<?>> readProperty(String name, Any value, PropertyType type) {
        try {
            switch (type) {
                case BOOLEAN:
                    return Option.of(IProperty.buildBoolean(name, value.toBoolean()));

                case STRING:
                    return Option.of(IProperty.buildString(name, value.toString()));
                case STRING_ARRAY:
                    return Option.of(IProperty.buildStringArray(name, value.as(String[].class)));
                case URL:
                    return Option.of(IProperty.buildUrl(name, value.toString()));

                case INTEGER:
                    return Option.of(IProperty.buildInteger(name, value.toInt()));
                case INTEGER_ARRAY:
                    return Option.of(IProperty.buildIntegerArray(name, value.as(Integer[].class)));
                case INTEGER_RANGE:
                    return Option.of(IProperty.buildIntegerRange(name,
                                                                 value.toInt(RangeMapping.RANGE_LOWER_BOUND),
                                                                 value.toInt(RangeMapping.RANGE_UPPER_BOUND)));
                case INTEGER_INTERVAL:
                    return Option.of(IProperty.buildIntegerInterval(name,
                                                                    value.toInt(RANGE_LOWER_BOUND),
                                                                    value.toInt(RANGE_UPPER_BOUND)));

                case LONG:
                    return Option.of(IProperty.buildLong(name, value.toLong()));
                case LONG_ARRAY:
                    return Option.of(IProperty.buildLongArray(name, value.as(Long[].class)));
                case LONG_RANGE:
                    return Option.of(IProperty.buildLongRange(name,
                                                              value.toLong(RangeMapping.RANGE_LOWER_BOUND),
                                                              value.toLong(RangeMapping.RANGE_UPPER_BOUND)));
                case LONG_INTERVAL:
                    return Option.of(IProperty.buildLongInterval(name,
                                                                 value.toLong(RANGE_LOWER_BOUND),
                                                                 value.toLong(RANGE_UPPER_BOUND)));

                case DOUBLE:
                    return Option.of(IProperty.buildDouble(name, value.toDouble()));
                case DOUBLE_ARRAY:
                    return Option.of(IProperty.buildDoubleArray(name, value.as(Double[].class)));
                case DOUBLE_RANGE:
                    return Option.of(IProperty.buildDoubleRange(name,
                                                                value.toDouble(RangeMapping.RANGE_LOWER_BOUND),
                                                                value.toDouble(RangeMapping.RANGE_UPPER_BOUND)));
                case DOUBLE_INTERVAL:
                    return Option.of(IProperty.buildDoubleInterval(name,
                                                                   value.toDouble(RANGE_LOWER_BOUND),
                                                                   value.toDouble(RANGE_UPPER_BOUND)));

                case DATE_ISO8601:
                    return Option.of(IProperty.buildDate(name, parseDate(value.toString())));
                case DATE_ARRAY:
                    return Option.of(IProperty.buildDateArray(name, toDateTimes(value.as(String[].class))));
                case DATE_RANGE:
                    return Option.of(IProperty.buildDateRange(name,
                                                              parseDate(value.toString(RangeMapping.RANGE_LOWER_BOUND)),
                                                              parseDate(value.toString(RangeMapping.RANGE_UPPER_BOUND))));
                case DATE_INTERVAL:
                    return Option.of(IProperty.buildDateInterval(name,
                                                                 toDateRange(value.toString(RANGE_LOWER_BOUND),
                                                                             value.toString(RANGE_UPPER_BOUND))));

                case OBJECT:
                    return Option.of(IProperty.buildObject(name, toSubProperties(value)));
                case JSON:
                    return Option.of(IProperty.buildJson(name,
                                                         getGson().fromJson(value.toString(), JsonElement.class)));

                default:
                    return Option.none();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Option.none();
        }
    }

    default IProperty<?>[] toSubProperties(Any value) {
        return HashMap.ofAll(value.asMap())
                      .toStream()
                      .flatMap(t -> getPropTypeFinder().getPropertyTypeForAttributeWithName(t._1())
                                                       .flatMap(type -> readProperty(t._1(), t._2(), type)))
                      .toJavaArray(IProperty[]::new);
    }

    default Range<OffsetDateTime> toDateRange(String from, String to) {
        return Range.open(parseDate(from), parseDate(to));
    }

    default OffsetDateTime parseDate(String from) {
        return Try.of(() -> OffsetDateTimeAdapter.parse(from)).getOrNull();
    }

    default OffsetDateTime[] toDateTimes(String[] as) {
        return Stream.of(as).map(this::parseDate).toArray(OffsetDateTime[]::new);
    }

}
