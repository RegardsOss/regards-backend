package fr.cnes.regards.modules.order.service.processing.correlation;

import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import io.vavr.control.Option;
import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.control.Option.none;
import static io.vavr.control.Option.some;

@Value
public class ProcessInputCorrelationIdentifier {

    BatchSuborderCorrelationIdentifier batchSuborderIdentifier;
    Option<String> featureIpId;
    Option<String> fileName;

    public static String repr(BatchSuborderCorrelationIdentifier batchSuborderIdentifier) {
        return new ProcessInputCorrelationIdentifier(batchSuborderIdentifier, none(), none()).repr();
    }

    public static String repr(BatchSuborderCorrelationIdentifier batchSuborderIdentifier, String featureIpId) {
        return new ProcessInputCorrelationIdentifier(batchSuborderIdentifier, some(featureIpId), none()).repr();
    }

    public static String repr(BatchSuborderCorrelationIdentifier batchSuborderIdentifier, EntityFeature feature) {
        return repr(batchSuborderIdentifier, feature.getId().toString());
    }

    public static String repr(BatchSuborderCorrelationIdentifier batchSuborderIdentifier, EntityFeature feature, DataFile file) {
        return repr(batchSuborderIdentifier, feature.getId().toString(), file.getFilename());
    }

    public static String repr(BatchSuborderCorrelationIdentifier batchSuborderIdentifier, String featureIpId, String fileName) {
        return new ProcessInputCorrelationIdentifier(batchSuborderIdentifier, some(featureIpId), some(fileName)).repr();
    }

    public String repr() {
        return String.format("file:/%s", batchSuborderIdentifier.repr())
                + (featureIpId.map(urn -> "/" + urn).getOrElse(""))
                + (fileName.map(f -> "/" + f).getOrElse(""));
    }

    private static final Pattern PATTERN = Pattern.compile("file:/(?<batch>[^/]+)(?:/(?<feature>[^/]+))?(?:/(?<file>.*))?");

    public static Option<ProcessInputCorrelationIdentifier> parse(String repr) {
        Matcher matcher = PATTERN.matcher(repr);
        if (matcher.matches()) {
            String batch = matcher.group("batch");
            return BatchSuborderCorrelationIdentifier.parse(batch)
                .map(batchCorrelationId -> {
                    Option<String> featureIpId = Option.of(matcher.group("feature"));
                    Option<String> fileName = Option.of(matcher.group("file"));
                    return new ProcessInputCorrelationIdentifier(batchCorrelationId, featureIpId, fileName);
                });
        }
        else {
            return none();
        }
    }
}
