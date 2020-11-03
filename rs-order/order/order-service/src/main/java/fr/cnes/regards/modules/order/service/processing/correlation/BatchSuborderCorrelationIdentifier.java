package fr.cnes.regards.modules.order.service.processing.correlation;

import io.vavr.control.Option;
import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
