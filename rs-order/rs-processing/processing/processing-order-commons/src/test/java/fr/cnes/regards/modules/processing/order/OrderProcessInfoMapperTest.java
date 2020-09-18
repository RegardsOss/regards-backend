package fr.cnes.regards.modules.processing.order;

import io.vavr.collection.List;
import org.junit.Test;

import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;

public class OrderProcessInfoMapperTest {

    OrderProcessInfoMapper mapper = new OrderProcessInfoMapper();

    @Test
    public void testFromTo() {
        List<OrderProcessInfo> rands = randomList(OrderProcessInfo.class, 1000);
        rands.forEach(pi -> assertThat(mapper.fromMap(mapper.toMap(pi))).contains(pi));
    }

}