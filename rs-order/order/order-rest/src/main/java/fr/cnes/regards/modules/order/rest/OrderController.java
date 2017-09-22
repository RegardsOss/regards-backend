package fr.cnes.regards.modules.order.rest;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EmptyBasketException;
import fr.cnes.regards.framework.module.rest.exception.NotYetAvailableException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.dto.OrderDto;
import fr.cnes.regards.modules.order.service.IBasketService;
import fr.cnes.regards.modules.order.service.IOrderService;

/**
 * Order controller
 * @author oroussel
 */
@RestController
@RequestMapping("")
public class OrderController implements IResourceController<OrderDto> {

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IBasketService basketService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private PagedResourcesAssembler<OrderDto> orderDtoPagedResourcesAssembler;

    private static final String ADMIN_ROOT_PATH = "/orders";

    private static final String USER_ROOT_PATH = "/user/orders";

    @ResourceAccess(description = "Validate current basket and find or create corresponding order",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, path = USER_ROOT_PATH)
    public ResponseEntity<Resource<OrderDto>> createOrder() throws EmptyBasketException {
        String user = authResolver.getUser();
        Basket basket = basketService.find(user);

        Order order = orderService.createOrder(basket);
        // Order has been created, basket can be emptied
        basketService.deleteIfExists(user);

        return new ResponseEntity<>(toResource(OrderDto.fromOrder(order)), HttpStatus.CREATED);
    }

    @ResourceAccess(description = "Retrieve specified order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = USER_ROOT_PATH + "/{orderId}")
    public ResponseEntity<Resource<OrderDto>> retrieveOrder(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(toResource(OrderDto.fromOrder(orderService.loadSimple(orderId))));
    }

    @ResourceAccess(description = "Find all or specified user orders")
    @RequestMapping(method = RequestMethod.GET, path = ADMIN_ROOT_PATH)
    public ResponseEntity<PagedResources<Resource<OrderDto>>> findAll(
            @RequestParam(value = "user", required = false) String user, Pageable pageRequest) {
        Page<Order> orderPage = (user.isEmpty()) ? orderService.findAll(pageRequest)
                : orderService.findAll(user, pageRequest);
        return ResponseEntity.ok(toPagedResources(orderPage.map(OrderDto::fromOrder), orderDtoPagedResourcesAssembler));
    }

    @ResourceAccess(description = "Find all user orders", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = USER_ROOT_PATH)
    public ResponseEntity<PagedResources<Resource<OrderDto>>> findAll(Pageable pageRequest) {
        String user = authResolver.getUser();
        return ResponseEntity.ok(toPagedResources(orderService.findAll(user, pageRequest).map(OrderDto::fromOrder),
                                                  orderDtoPagedResourcesAssembler));
    }

    @ResourceAccess(description = "Download a Zip file containing all currently available files",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = USER_ROOT_PATH + "/{orderId}/download")
    public void downloadAllAvailableFiles(@PathVariable("orderId") Long orderId, HttpServletResponse response)
            throws NotYetAvailableException {
        orderService.downloadOrderCurrentZip(orderId, response);
    }

    // TODO : add links
    @Override
    public Resource<OrderDto> toResource(OrderDto order, Object... extras) {
        return resourceService.toResource(order);
    }
}
