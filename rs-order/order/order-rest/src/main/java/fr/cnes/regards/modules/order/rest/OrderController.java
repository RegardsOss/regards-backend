package fr.cnes.regards.modules.order.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EmptyBasketException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.service.IBasketService;
import fr.cnes.regards.modules.order.service.IOrderService;

/**
 * Order controller
 *
 * @author oroussel
 */
@RestController
@RequestMapping("/orders")
public class OrderController implements IResourceController<Order> {
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IBasketService basketService;

    @Autowired
    private IOrderService orderService;

    @ResourceAccess(description = "Validate current basket and findOrCreate corresponding order")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Resource<Order>> createOrder() throws EmptyBasketException {
        Basket basket = basketService.find(SecurityUtils.getActualUser());

        Order order = orderService.createOrder(basket);

        return new ResponseEntity<>(toResource(order), HttpStatus.CREATED);
    }

    @Override
    public Resource<Order> toResource(Order order, Object... extras) {
        return resourceService.toResource(order);
    }
}
