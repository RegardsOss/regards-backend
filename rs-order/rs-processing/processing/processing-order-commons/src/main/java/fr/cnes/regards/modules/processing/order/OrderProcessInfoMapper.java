package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.DataType;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import static fr.cnes.regards.modules.processing.order.Constants.*;

public class OrderProcessInfoMapper extends AbstractMapper<OrderProcessInfo> {

    public Map<String, String> toMap(OrderProcessInfo params) {
        return HashMap.of(
            SCOPE, params.getScope().name(),
            CARDINALITY, params.getCardinality().name(),
            DATATYPES, params.getRequiredDatatypes()
                .map(DataType::toString)
                .reduceOption((a,b) -> String.format("%s,%s", a, b))
                .getOrElse(""),
            SIZE_LIMIT_TYPE, params.getSizeLimit().getType().name(),
            SIZE_LIMIT_VALUE, params.getSizeLimit().getLimit().toString()
        );
    }

    public Option<OrderProcessInfo> fromMap(Map<String, String> map) {
        return parse(map, SCOPE, Scope.class)
            .flatMap(scope -> parse(map, CARDINALITY, Cardinality.class)
                .flatMap(card -> parseDatatypes(map)
                    .flatMap(datatypes -> parseSizeLimit(map)
                        .map(sizeLimit -> new OrderProcessInfo(scope, card, datatypes, sizeLimit)))));
    }

    protected Option<List<DataType>> parseDatatypes(Map<String, String> map) {
        return map.get(DATATYPES)
            .map(str -> str.split(","))
            .map(List::of)
            .map(strs -> strs.filter(StringUtils::isNotBlank).map(String::trim))
            .map(strs -> strs.flatMap(str -> parse(DataType.class, str).toList()));
    }

    protected Option<SizeLimit> parseSizeLimit(Map<String, String> map) {
        return map.get(SIZE_LIMIT_VALUE)
                .flatMap(str -> Try.of(() -> Long.parseLong(str)).toOption())
                .flatMap(value -> map.get(SIZE_LIMIT_TYPE)
                    .flatMap(str -> parse(SizeLimit.Type.class, str))
                    .map(type -> new SizeLimit(type, value)));
    }

}
