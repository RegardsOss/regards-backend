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
import java.time.OffsetDateTime;

public class ICriterionJsoniterDecoder implements NullSafeDecoderBuilder {

    public static final String VALUE = "value";

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
            if (critType.equals(EmptyCriterion.class)) { return ICriterion.all(); }
            else if (critType.equals(AndCriterion.class)) {
                return ICriterion.and(List.ofAll(criterion.get("criterions").asList())
                    .map(c -> c.as(ICriterion.class)));
            }
            else if (critType.equals(OrCriterion.class)) {
                return ICriterion.or(List.ofAll(criterion.get("criterions").asList())
                        .map(c -> c.as(ICriterion.class)));
            }
            else if (critType.equals(NotCriterion.class)) {
                return ICriterion.not(criterion.as(ICriterion.class, "criterion"));
            }
            else if (critType.equals(StringMatchCriterion.class)) {
                return new StringMatchCriterion(
                        criterion.toString("name"),
                        criterion.as(MatchType.class, "type"),
                        criterion.toString(VALUE),
                        criterion.as(StringMatchType.class, "matchType")
                );
            }
            else if (critType.equals(StringMatchAnyCriterion.class)) {
                return new StringMatchAnyCriterion(
                        criterion.toString("name"),
                        criterion.as(StringMatchType.class, "matchType"),

                        List.ofAll(criterion.get(VALUE).asList()).map(Any::toString).toJavaArray(String[]::new)
                );
            }
            else if (critType.equals(StringMultiMatchCriterion.class)) {
                return new StringMultiMatchCriterion(
                        List.ofAll(criterion.get("names").asList()).map(Any::toString).toJavaSet(),
                        MultiMatchQueryBuilder.Type.valueOf(criterion.toString("type")),
                        criterion.toString(VALUE)
                );
            }
            else if (critType.equals(DateMatchCriterion.class)) {
                return new DateMatchCriterion(
                        criterion.toString("name"),
                        OffsetDateTimeAdapter.parse(criterion.toString(VALUE))
                );
            }
            else if (critType.equals(DateRangeCriterion.class)) {
                DateRangeCriterion result = new DateRangeCriterion(criterion.toString("name"));
                criterion.get("valueComparisons").asList().forEach(vc ->
                        result.addValueComparison(vc.as(new TypeLiteral<ValueComparison<OffsetDateTime>>(){})));
                return result;
            }
            else if (critType.equals(IntMatchCriterion.class)) {
                return new IntMatchCriterion(
                        criterion.toString("name"),
                        criterion.toInt(VALUE)
                );
            }
            else if (critType.equals(LongMatchCriterion.class)) {
                return new LongMatchCriterion(
                        criterion.toString("name"),
                        criterion.toLong(VALUE)
                );
            }
            else if (critType.equals(BooleanMatchCriterion.class)) {
                return new BooleanMatchCriterion(
                        criterion.toString("name"),
                        criterion.toBoolean(VALUE)
                );
            }
            else if (critType.equals(FieldExistsCriterion.class)) {
                return new FieldExistsCriterion(criterion.toString("name"));
            }
            else if (critType.equals(BoundaryBoxCriterion.class)) {
                return new BoundaryBoxCriterion(
                        criterion.toDouble("minX"),
                        criterion.toDouble("minY"),
                        criterion.toDouble("maxX"),
                        criterion.toDouble("maxY")
                );
            }
            else if (critType.equals(CircleCriterion.class)) {
                return new CircleCriterion(
                        criterion.as(double[].class, "coordinates"),
                        criterion.toString("radius")
                );
            }
            else if (critType.equals(PolygonCriterion.class)) {
                return new PolygonCriterion(criterion.as(double[][][].class, "coordinates"));
            }
            else {
                return criterion.as(critType);
            }
        }
        catch(Exception e) {
            throw new IOException(e);
        }
    }

}
