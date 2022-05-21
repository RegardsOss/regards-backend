package fr.cnes.regards.framework.jsoniter;

import com.jsoniter.output.EncodingMode;
import com.jsoniter.spi.Config;
import com.jsoniter.spi.DecodingMode;

public class IIndexableJsoniterConfig extends Config {

    public static final String CONFIG_NAME = IIndexableJsoniterConfig.class.getName();

    public IIndexableJsoniterConfig() {
        super(CONFIG_NAME, getBuilder());
    }

    private static Builder getBuilder() {
        return new Builder().decodingMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH)
                            .encodingMode(EncodingMode.DYNAMIC_MODE);
    }

}
