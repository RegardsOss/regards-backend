/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.processing.correlation;

import io.vavr.control.Option;
import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines information present in, and ser/deser for, a processing Batch correlation ID.
 *
 * @author Guillaume Andrieu
 */
@Value
public class BatchSuborderCorrelationIdentifier {

    Long orderId;
    Long dsSelId;
    Long subOrderId;

    public String repr() {
        return String.format("order-%d_dsSel-%d_subOrder-%d", orderId, dsSelId, subOrderId);
    }

    private static final Pattern PARSE_FORMAT =
            Pattern.compile("order-(?<orderId>\\d+)_dsSel-(?<dsSelId>\\d+)_subOrder-(?<subOrderId>\\d+)");

    public static Option<BatchSuborderCorrelationIdentifier> parse(String repr) {
        Matcher matcher = PARSE_FORMAT.matcher(repr);
        if (matcher.matches()) {
            return Option.some(new BatchSuborderCorrelationIdentifier(
                Long.parseLong(matcher.group("orderId"), 10),
                Long.parseLong(matcher.group("dsSelId"), 10),
                Long.parseLong(matcher.group("subOrderId"), 10)
            ));
        }
        else {
            return Option.none();
        }
    }
}
