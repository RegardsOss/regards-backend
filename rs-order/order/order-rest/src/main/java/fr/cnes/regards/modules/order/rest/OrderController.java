package fr.cnes.regards.modules.order.rest;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.base.Strings;
import feign.Body;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.dto.OrderDto;
import fr.cnes.regards.modules.order.domain.exception.CannotDeleteOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotPauseOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotRemoveOrderException;
import fr.cnes.regards.modules.order.domain.exception.CannotResumeOrderException;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.NotYetAvailableException;
import fr.cnes.regards.modules.order.service.IBasketService;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;

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
    private IOrderDataFileService dataFileService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private PagedResourcesAssembler<OrderDto> orderDtoPagedResourcesAssembler;

    public static final String ADMIN_ROOT_PATH = "/orders";

    public static final String USER_ROOT_PATH = "/user/orders";

    public static final String REMOVE_ORDER_PATH = USER_ROOT_PATH + "/remove/{orderId}";

    public static final String DELETE_ORDER_PATH = USER_ROOT_PATH + "/{orderId}";

    public static final String RESUME_ORDER_PATH = USER_ROOT_PATH + "/resume/{orderId}";

    public static final String PAUSE_ORDER_PATH = USER_ROOT_PATH + "/pause/{orderId}";

    public static final String GET_ORDER_PATH = USER_ROOT_PATH + "/{orderId}";

    public static final String ZIP_DOWNLOAD_PATH = USER_ROOT_PATH + "/{orderId}/download";

    public static final String METALINK_DOWNLOAD_PATH = USER_ROOT_PATH + "/{orderId}/metalink/download";

    public static final String PUBLIC_METALINK_DOWNLOAD_PATH = USER_ROOT_PATH + "/metalink/download";

    @Value("${regards.order.secret}")
    private String secret;

    @ResourceAccess(description = "Validate current basket and create corresponding order",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, path = USER_ROOT_PATH)
    public ResponseEntity<Resource<OrderDto>> createOrder(@RequestBody @RequestParam(name = "onSuccessUrl") String url)
            throws EmptyBasketException {
        String user = authResolver.getUser();
        Basket basket = basketService.find(user);

        Order order = orderService.createOrder(basket, url);
        // Order has been created, basket can be emptied
        basketService.deleteIfExists(user);

        return new ResponseEntity<>(toResource(OrderDto.fromOrder(order)), HttpStatus.CREATED);
    }

    @ResourceAccess(description = "Retrieve specified order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = GET_ORDER_PATH)
    public ResponseEntity<Resource<OrderDto>> retrieveOrder(@PathVariable("orderId") Long orderId) {
        Order order = orderService.loadSimple(orderId);
        if (order != null) {
            return ResponseEntity.ok(toResource(OrderDto.fromOrder(order)));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @ResourceAccess(description = "Resume an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = RESUME_ORDER_PATH)
    public ResponseEntity<Void> resumeOrder(@PathVariable("orderId") Long orderId) throws CannotResumeOrderException {
        orderService.resume(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Ask for an order to be paused", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = PAUSE_ORDER_PATH)
    public ResponseEntity<Void> pauseOrder(@PathVariable("orderId") Long orderId) throws CannotPauseOrderException {
        orderService.pause(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.DELETE, path = DELETE_ORDER_PATH)
    public ResponseEntity<Void> deleteOrder(@PathVariable("orderId") Long orderId) throws CannotDeleteOrderException {
        orderService.delete(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Remove an order", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.DELETE, path = REMOVE_ORDER_PATH)
    public ResponseEntity<Void> removeOrder(@PathVariable("orderId") Long orderId) throws CannotRemoveOrderException {
        orderService.remove(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Find all specified user orders or all users orders",
            role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.GET, path = ADMIN_ROOT_PATH)
    public ResponseEntity<PagedResources<Resource<OrderDto>>> findAll(
            @RequestParam(value = "user", required = false) String user, Pageable pageRequest) {
        Page<Order> orderPage = (Strings.isNullOrEmpty(user)) ?
                orderService.findAll(pageRequest) :
                orderService.findAll(user, pageRequest);
        return ResponseEntity.ok(toPagedResources(orderPage.map(OrderDto::fromOrder), orderDtoPagedResourcesAssembler));
    }

    @ResourceAccess(description = "Generate a CSV file with all orders", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.GET, path = ADMIN_ROOT_PATH, produces = "text/csv")
    public void generateCsv(HttpServletResponse response) throws IOException {
        orderService.writeAllOrdersInCsv(new BufferedWriter(response.getWriter()));
    }

    @ResourceAccess(description = "Find all user current orders", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = USER_ROOT_PATH)
    public ResponseEntity<PagedResources<Resource<OrderDto>>> findAll(Pageable pageRequest) {
        String user = authResolver.getUser();
        return ResponseEntity.ok(toPagedResources(
                orderService.findAll(user, pageRequest, OrderStatus.DELETED, OrderStatus.REMOVED)
                        .map(OrderDto::fromOrder), orderDtoPagedResourcesAssembler));
    }

    @ResourceAccess(description = "Download a Zip file containing all currently available files",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ZIP_DOWNLOAD_PATH)
    public ResponseEntity<StreamingResponseBody> downloadAllAvailableFiles(@PathVariable("orderId") Long orderId,
            HttpServletResponse response) throws EntityNotFoundException, IOException {
        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }
        response.addHeader("Content-disposition",
                           "attachment;filename=order_" + OffsetDateTime.now().toString() + ".zip");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(orderId));
        // No file available
        if (availableFiles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Stream the response
        return new ResponseEntity<>(os -> orderService.downloadOrderCurrentZip(order.getOwner(), availableFiles, os),
                                    HttpStatus.OK);
    }

    @ResourceAccess(description = "Download a Metalink file containing all files", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = METALINK_DOWNLOAD_PATH)
    public ResponseEntity<StreamingResponseBody> downloadMetalinkFile(@PathVariable("orderId") Long orderId,
            HttpServletResponse response) throws NotYetAvailableException, EntityNotFoundException {
        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }
        return createMetalinkDownloadResponse(orderId, response);
    }

    @ResourceAccess(description = "Download a metalink file containing all files granted by a token",
            role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = PUBLIC_METALINK_DOWNLOAD_PATH)
    public ResponseEntity<StreamingResponseBody> publicDownloadMetalinkFile(
            @RequestParam(name = IOrderService.ORDER_TOKEN) String token, HttpServletResponse response)
            throws NotYetAvailableException, EntityNotFoundException {
        Long orderId;
        try {
            Claims claims = jwtService.parseToken(token, secret);
            orderId = Long.parseLong(claims.get(IOrderService.ORDER_ID_KEY, String.class));
        } catch (InvalidJwtException | MalformedJwtException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }

        return createMetalinkDownloadResponse(orderId, response);
    }

    /**
     * Fill Response headers and create streaming response
     */
    private ResponseEntity<StreamingResponseBody> createMetalinkDownloadResponse(@PathVariable("orderId") Long orderId,
            HttpServletResponse response) {
        response.addHeader("Content-disposition",
                           "attachment;filename=order_" + OffsetDateTime.now().toString() + ".metalink");
        response.setContentType("application/metalink+xml");

        // Stream the response
        return new ResponseEntity<>(os -> orderService.downloadOrderMetalink(orderId, os), HttpStatus.OK);
    }

    // TODO : add links
    @Override
    public Resource<OrderDto> toResource(OrderDto order, Object... extras) {
        return resourceService.toResource(order);
    }
}
