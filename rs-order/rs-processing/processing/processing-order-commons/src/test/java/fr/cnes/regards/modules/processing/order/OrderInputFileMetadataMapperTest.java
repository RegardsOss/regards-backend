package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import io.vavr.collection.List;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderInputFileMetadataMapperTest {

    OrderInputFileMetadataMapper mapper = new OrderInputFileMetadataMapper();

    @Test
    public void testFromTo() {
        UniformResourceName urn = UniformResourceName.build("theidentifier", EntityType.DATA, "theTenant", UUID.randomUUID(), 5, 5L, "test");
        OrderInputFileMetadata oifmdTrue = new OrderInputFileMetadata(true, urn);
        OrderInputFileMetadata oifmdFalse = new OrderInputFileMetadata(false, urn);

        List<OrderInputFileMetadata> rands = List.of(oifmdTrue, oifmdFalse);

        rands.forEach(pi -> assertThat(mapper.fromMap(mapper.toMap(pi))).contains(pi));
    }

}