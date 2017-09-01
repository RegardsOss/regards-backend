package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;

/**
 * Order service
 * @author oroussel
 */
public interface IOrderService {
    Order createOrder(Basket basket);
}
