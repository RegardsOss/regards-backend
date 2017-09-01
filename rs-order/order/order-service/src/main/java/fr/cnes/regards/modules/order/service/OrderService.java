package fr.cnes.regards.modules.order.service;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;

/**
 * @author oroussel
 */
@Service
public class OrderService implements IOrderService {
    @Autowired
    private IOrderRepository repos;

    @Override
    public Order createOrder(Basket basket) {
        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setEmail(basket.getEmail());
        return repos.save(order);
    }
}
