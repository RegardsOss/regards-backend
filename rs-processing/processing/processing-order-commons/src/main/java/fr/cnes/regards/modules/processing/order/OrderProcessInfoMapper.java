/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.forecast.ForecastParser;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import static fr.cnes.regards.modules.processing.order.Constants.*;

/**
 * This class is a mapper for {@link OrderProcessInfo}.
 *
 * @author gandrieu
 */
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
            SIZE_LIMIT_VALUE, params.getSizeLimit().getLimit().toString(),
            SIZE_FORECAST, params.getSizeForecast().format()
        );
    }

    public Option<OrderProcessInfo> fromMap(Map<String, String> map) {
        return parse(map, SCOPE, Scope.class)
            .flatMap(scope -> parse(map, CARDINALITY, Cardinality.class)
                .flatMap(card -> parseDatatypes(map)
                    .flatMap(datatypes -> parseSizeLimit(map)
                        .flatMap(sizeLimit -> parseSizeForecast(map)
                            .map(sizeForecast -> new OrderProcessInfo(scope, card, datatypes, sizeLimit, sizeForecast))))));
    }

    protected Option<IResultSizeForecast> parseSizeForecast(Map<String, String> map) {
        return map.get(SIZE_FORECAST)
            .flatMap(fc -> ForecastParser.INSTANCE.parseResultSizeForecast(fc).toOption());
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
