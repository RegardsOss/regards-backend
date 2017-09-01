package fr.cnes.regards.modules.order.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.OAISIdentifier;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class OrderServiceIT {
    @Autowired
    private IOrderService orderService;

    @Autowired
    private IBasketRepository basketRepository;

    private static final String USER_EMAIL = "leo.mieulet@margoulin.com";

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
                                                                                "ORDER", UUID.randomUUID(), 1);

    @Test
    public void test1() throws Exception {
        Basket basket = new Basket();
        basket.setEmail(USER_EMAIL);

        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setOpenSearchRequest("someone:something");
        dsSelection.setDatasetLabel("DS1");
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setFilesSize(1_000_000l);
        dsSelection.setFilesCount(1);
        dsSelection.setObjectsCount(1);

        BasketDatedItemsSelection itemsSelection = new BasketDatedItemsSelection();
        itemsSelection.setFilesSize(1_000_000l);
        itemsSelection.setFilesCount(1);
        itemsSelection.setObjectsCount(1);
        itemsSelection.setOpenSearchRequest("someone:something");
        itemsSelection.setDate(OffsetDateTime.now());
        dsSelection.setItemsSelections(Sets.newTreeSet(Collections.singleton(itemsSelection)));
        basket.setDatasetSelections(Sets.newTreeSet(Collections.singleton(dsSelection)));

        basket =  basketRepository.save(basket);

        Order order = orderService.createOrder(basket);
        Assert.assertNotNull(order);
    }
}
