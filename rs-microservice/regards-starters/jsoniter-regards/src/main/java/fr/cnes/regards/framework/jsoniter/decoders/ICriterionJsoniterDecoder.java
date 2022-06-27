package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.TypeLiteral;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.indexer.domain.criterion.*;
import io.vavr.collection.List;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

public class ICriterionJsoniterDecoder implements NullSafeDecoderBuilder {

    public static final String VALUE = "value";

    public static final String OPERATOR = "operator";

    public static Decoder selfRegister() {
        Decoder decoder = new ICriterionJsoniterDecoder().nullSafe();
        JsoniterSpi.registerTypeDecoder(ICriterion.class, decoder);
        return decoder;
    }

    @Override
    public Object decode(JsonIterator iter) throws IOException {
        Any criterion = iter.readAny();
        String type = criterion.toString("@type@");
        try {
            Class<?> critType = Class.forName(type);
            if (critType.equals(EmptyCriterion.class)) {
                return ICriterion.all();
            } else if (critType.equals(AndCriterion.class)) {
                return ICriterion.and(List.ofAll(criterion.get("criterions").asList())
                                          .map(c -> c.as(ICriterion.class)));
            } else if (critType.equals(OrCriterion.class)) {
                return ICriterion.or(List.ofAll(criterion.get("criterions").asList()).map(c -> c.as(ICriterion.class)));
            } else if (critType.equals(NotCriterion.class)) {
                return ICriterion.not(criterion.as(ICriterion.class, "criterion"));
            } else if (critType.equals(StringMatchCriterion.class)) {
                Any name = criterion.get("name");
                Any stringMatchCriterionType = criterion.get("type");
                Any value = criterion.get(VALUE);
                Any matchType = criterion.get("matchType");
                if (isNull(matchType)) {
                    matchType = Any.wrap(StringMatchType.KEYWORD);
                }
                return new StringMatchCriterion(name.toString(),
                                                stringMatchCriterionType.as(MatchType.class),
                                                value.toString(),
                                                StringMatchType.valueOf(matchType.toString()));
            } else if (critType.equals(StringMatchAnyCriterion.class)) {
                return new StringMatchAnyCriterion(criterion.toString("name"),
                                                   criterion.as(StringMatchType.class, "matchType"),
                                                   List.ofAll(criterion.get(VALUE).asList())
                                                       .map(Any::toString)
                                                       .toJavaArray(String[]::new));
            } else if (critType.equals(StringMultiMatchCriterion.class)) {
                return new StringMultiMatchCriterion(List.ofAll(criterion.get("names").asList())
                                                         .map(Any::toString)
                                                         .toJavaSet(),
                                                     MultiMatchQueryBuilder.Type.valueOf(criterion.toString("type")),
                                                     criterion.toString(VALUE));
            } else if (critType.equals(DateMatchCriterion.class)) {
                return new DateMatchCriterion(criterion.toString("name"),
                                              OffsetDateTimeAdapter.parse(criterion.toString(VALUE)));
            } else if (critType.equals(DateRangeCriterion.class)) {
                DateRangeCriterion result = new DateRangeCriterion(criterion.toString("name"));
                criterion.get("valueComparisons")
                         .asList()
                         .forEach(vc -> result.addValueComparison(vc.as(new TypeLiteral<>() {

                         })));
                return result;
            } else if (critType.equals(IntMatchCriterion.class)) {
                return new IntMatchCriterion(criterion.toString("name"), criterion.toInt(VALUE));
            } else if (critType.equals(LongMatchCriterion.class)) {
                return new LongMatchCriterion(criterion.toString("name"), criterion.toLong(VALUE));
            } else if (critType.equals(BooleanMatchCriterion.class)) {
                return new BooleanMatchCriterion(criterion.toString("name"), criterion.toBoolean(VALUE));
            } else if (critType.equals(FieldExistsCriterion.class)) {
                return new FieldExistsCriterion(criterion.toString("name"));
            } else if (critType.equals(BoundaryBoxCriterion.class)) {
                return new BoundaryBoxCriterion(criterion.toDouble("minX"),
                                                criterion.toDouble("minY"),
                                                criterion.toDouble("maxX"),
                                                criterion.toDouble("maxY"));
            } else if (critType.equals(CircleCriterion.class)) {
                return new CircleCriterion(criterion.as(double[].class, "coordinates"), criterion.toString("radius"));
            } else if (critType.equals(PolygonCriterion.class)) {
                return new PolygonCriterion(criterion.as(double[][][].class, "coordinates"));
            } else if (critType.equals(RangeCriterion.class)) {
                String attributeName = criterion.toString("name");
                java.util.List<Any> values = criterion.get("valueComparisons").asList();
                if (values.size() == 2) {
                    return getBetweenCriterion(attributeName, values.get(0), values.get(1));
                } else {
                    return getRangeCriterion(attributeName, values.get(0));
                }
            } else {
                return criterion.as(critType);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Number & Comparable<T>> ICriterion getRangeCriterion(String attributeName, Any valueComparision)
        throws IOException {
        ComparisonOperator operator = valueComparision.get(OPERATOR).as(ComparisonOperator.class);
        T number;
        try {
            Number value = NumberFormat.getInstance().parse(valueComparision.get(VALUE).toString());
            if (value instanceof Double doubleValue) {
                number = (T) doubleValue;
            } else if (value instanceof Integer integerValue) {
                number = (T) integerValue;
            } else if (value instanceof Long longValue) {
                number = (T) longValue;
            } else if (value instanceof Short shortValue) {
                number = (T) shortValue;
            } else if (value instanceof Byte byteValue) {
                number = (T) byteValue;
            } else if (value instanceof Float floatValue) {
                number = (T) floatValue;
            } else {
                throw new IOException("value could not be handled: " + value);
            }
        } catch (ParseException e) {
            throw new IOException("value could not be handled", e);
        }
        return switch (operator) {
            case GREATER -> ICriterion.gt(attributeName, number);
            case GREATER_OR_EQUAL -> ICriterion.ge(attributeName, number);
            case LESS -> ICriterion.lt(attributeName, number);
            case LESS_OR_EQUAL -> ICriterion.le(attributeName, number);
            default -> throw new IOException("Value comparison operator is not handled: " + operator);
        };
    }

    private ICriterion getBetweenCriterion(String attributeName,
                                           Any lowerBound,
                                           Any upperBound) throws IOException {
        ComparisonOperator lowerOperator = lowerBound.get(OPERATOR).as(ComparisonOperator.class);
        ComparisonOperator upperOperator = upperBound.get(OPERATOR).as(ComparisonOperator.class);
        String lowerValueAsString = lowerBound.get(VALUE).toString();
        String upperValueAsString = upperBound.get(VALUE).toString();
        try {
            Number lower = NumberFormat.getInstance().parse(lowerValueAsString);
            Number upper = NumberFormat.getInstance().parse(upperValueAsString);
            if (lower instanceof Double lowerDouble) {
                return ICriterion.between(attributeName,
                                          lowerDouble,
                                          lowerOperator == ComparisonOperator.GREATER_OR_EQUAL
                                          || lowerOperator == ComparisonOperator.LESS_OR_EQUAL,
                                          upper.doubleValue(),
                                          upperOperator == ComparisonOperator.GREATER_OR_EQUAL
                                          || upperOperator == ComparisonOperator.LESS_OR_EQUAL);

            } else if (lower instanceof Integer integerLower) {
                return ICriterion.between(attributeName,
                                          integerLower,
                                          lowerOperator == ComparisonOperator.GREATER_OR_EQUAL
                                          || lowerOperator == ComparisonOperator.LESS_OR_EQUAL,
                                          upper.intValue(),
                                          upperOperator == ComparisonOperator.GREATER_OR_EQUAL
                                          || upperOperator == ComparisonOperator.LESS_OR_EQUAL);
            } else if (lower instanceof Long longLower) {
                return ICriterion.between(attributeName,
                                          longLower,
                                          lowerOperator == ComparisonOperator.GREATER_OR_EQUAL
                                          || lowerOperator == ComparisonOperator.LESS_OR_EQUAL,
                                          upper.longValue(),
                                          upperOperator == ComparisonOperator.GREATER_OR_EQUAL
                                          || upperOperator == ComparisonOperator.LESS_OR_EQUAL);
            } else {
                throw new IOException("Lower value could not be handled: " + lower.getClass());
            }
        } catch (ParseException e) {
            throw new IOException(String.format("lower value or upper value could not be handled. lower: %s upper: %s",
                                                lowerValueAsString,
                                                upperValueAsString), e);
        }
    }

}
