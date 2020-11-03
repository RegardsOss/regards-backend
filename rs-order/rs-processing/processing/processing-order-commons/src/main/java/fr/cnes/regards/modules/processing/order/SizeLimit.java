package fr.cnes.regards.modules.processing.order;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

@With @Value @AllArgsConstructor
public class SizeLimit {

    public enum Type {
        NO_LIMIT, FILES, BYTES;
    }

    Type type;
    Long limit;

}
