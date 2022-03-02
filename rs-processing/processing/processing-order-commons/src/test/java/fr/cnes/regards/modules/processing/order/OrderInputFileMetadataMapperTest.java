package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderInputFileMetadataMapperTest {

    OrderInputFileMetadataMapper mapper = new OrderInputFileMetadataMapper();

    @Test
    public void testFromMapIdempotence() {
        UniformResourceName urn = UniformResourceName.build("theidentifier", EntityType.DATA, "theTenant", UUID.randomUUID(), 5, 5L, "test");
        OrderInputFileMetadata oifmdTrue = new OrderInputFileMetadata(true, urn, null);
        OrderInputFileMetadata oifmdFalse = new OrderInputFileMetadata(false, urn, null);
        OrderInputFileMetadata oifmdTrueWithStorePath = new OrderInputFileMetadata(true, urn, "toto");
        OrderInputFileMetadata oifmdFalseWithStorePath = new OrderInputFileMetadata(false, urn, "toto");

        List<OrderInputFileMetadata> rands = List.of(oifmdTrue, oifmdFalse, oifmdTrueWithStorePath, oifmdFalseWithStorePath);

        rands.forEach(pi -> assertThat(mapper.fromMap(mapper.toMap(pi))).contains(pi));
    }

    @Test
    public void testFromMapWithGsonDeserializedObj() {
        UniformResourceName urn = UniformResourceName.build("theidentifier", EntityType.DATA, "theTenant", UUID.randomUUID(), 5, 5L, "test");

        Map<String, String> oifmdTrueAsMap = HashMap.of(Constants.INTERNAL, "true", Constants.FEATURE_ID, urn.toString());
        OrderInputFileMetadata oifmdTrue = new OrderInputFileMetadata(true, urn, null);
        assertThat(mapper.fromMap(oifmdTrueAsMap)).contains(oifmdTrue);

        Map<String, String> oifmdFalseAsMap = HashMap.of(Constants.INTERNAL, "false", Constants.FEATURE_ID, urn.toString());
        OrderInputFileMetadata oifmdFalse = new OrderInputFileMetadata(false, urn, null);
        assertThat(mapper.fromMap(oifmdFalseAsMap)).contains(oifmdFalse);

        Map<String, String> oifmdTrueWithStorePathAsMap = HashMap.of(Constants.INTERNAL, "true", Constants.FEATURE_ID, urn.toString(), Constants.STORED_PATH, "toto");
        OrderInputFileMetadata oifmdTrueWithStorePath = new OrderInputFileMetadata(true, urn, "toto");
        assertThat(mapper.fromMap(oifmdTrueWithStorePathAsMap)).contains(oifmdTrueWithStorePath);

        Map<String, String> oifmdFalseWithStorePathAsMap = HashMap.of(Constants.INTERNAL, "false", Constants.FEATURE_ID, urn.toString(), Constants.STORED_PATH, "toto");
        OrderInputFileMetadata oifmdFalseWithStorePath = new OrderInputFileMetadata(false, urn, "toto");
        assertThat(mapper.fromMap(oifmdFalseWithStorePathAsMap)).contains(oifmdFalseWithStorePath);
    }
}