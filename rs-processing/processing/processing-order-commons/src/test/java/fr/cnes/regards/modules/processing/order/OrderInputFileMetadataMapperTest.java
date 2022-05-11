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
    public void testFromTo() {
        UniformResourceName urn = getSomeUrn();
        OrderInputFileMetadata oifmdTrue = new OrderInputFileMetadata(true, urn, null);
        OrderInputFileMetadata oifmdFalse = new OrderInputFileMetadata(false, urn, null);

        List<OrderInputFileMetadata> rands = List.of(oifmdTrue, oifmdFalse);

        rands.forEach(pi -> assertThat(mapper.fromMap(mapper.toMap(pi))).contains(pi));
    }

    @Test
    public void test_valid_order_input_file_metadata_when_stored_path_not_present() {
        Map<String, String> someMap = HashMap.of(Constants.INTERNAL,
                                                 "true",
                                                 Constants.FEATURE_ID,
                                                 getSomeUrn().toString());
        assertThat(mapper.fromMap(someMap).isDefined()).isTrue();
    }

    private UniformResourceName getSomeUrn() {
        return UniformResourceName.build("theidentifier",
                                         EntityType.DATA,
                                         "theTenant",
                                         UUID.randomUUID(),
                                         5,
                                         5L,
                                         "test");
    }

}