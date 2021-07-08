/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.order.rest;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.dto.OrderDto;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.service.*;
import fr.cnes.regards.modules.order.service.settings.IOrderSettingsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Encoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order controller
 *
 * @author oroussel
 */
@RestController
public class OrderController implements IResourceController<OrderDto> {

    public static class OrderRequest {

        private String label;

        private String onSuccessUrl;

        public OrderRequest() {
        }

        public OrderRequest(String label, String onSuccessUrl) {
            this.label = label;
            this.onSuccessUrl = onSuccessUrl;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getOnSuccessUrl() {
            return onSuccessUrl;
        }

        public void setOnSuccessUrl(String onSuccessUrl) {
            this.onSuccessUrl = onSuccessUrl;
        }
    }

    public static final String ADMIN_ROOT_PATH = "/orders";

    public static final String CSV = "/csv";

    public static final String USER_ROOT_PATH = "/user/orders";

    public static final String REMOVE_ORDER_PATH = USER_ROOT_PATH + "/remove/{orderId}";

    public static final String DELETE_ORDER_PATH = USER_ROOT_PATH + "/{orderId}";

    public static final String RESUME_ORDER_PATH = USER_ROOT_PATH + "/resume/{orderId}";

    public static final String PAUSE_ORDER_PATH = USER_ROOT_PATH + "/pause/{orderId}";

    public static final String RESTART_ORDER_PATH = USER_ROOT_PATH + "/{orderId}/restart";

    public static final String RETRY_ORDER_PATH = USER_ROOT_PATH + "/{orderId}/retry";

    public static final String GET_ORDER_PATH = USER_ROOT_PATH + "/{orderId}";

    public static final String ZIP_DOWNLOAD_PATH = USER_ROOT_PATH + "/{orderId}/download";

    public static final String METALINK_DOWNLOAD_PATH = USER_ROOT_PATH + "/{orderId}/metalink/download";

    public static final String PUBLIC_METALINK_DOWNLOAD_PATH = USER_ROOT_PATH + "/metalink/download";

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IBasketService basketService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderDownloadService orderDownloadService;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private IOrderSettingsService orderSettingsService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private PagedResourcesAssembler<OrderDto> orderDtoPagedResourcesAssembler;

    @Value("${regards.order.secret}")
    private String secret;

    @ResourceAccess(description = "Validate current basket and create corresponding order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, path = USER_ROOT_PATH)
    public ResponseEntity<EntityModel<OrderDto>> createOrder(@RequestBody OrderRequest orderRequest)
            throws IllegalStateException, EntityInvalidException, EmptyBasketException {
        Basket basket = basketService.find(authResolver.getUser());
        Order order = orderService.createOrder(
                basket,
                orderRequest.getLabel(),
                orderRequest.getOnSuccessUrl(),
                orderSettingsService.getUserOrderParameters().getSubOrderDuration()
        );
        return new ResponseEntity<>(toResource(OrderDto.fromOrder(order)), HttpStatus.CREATED);
    }

    @ResourceAccess(description = "Validate current basket and create corresponding order", role = DefaultRole.ADMIN)
    @RequestMapping(method = RequestMethod.POST, path = ADMIN_ROOT_PATH)
    public ResponseEntity<EntityModel<OrderDto>> createAppOrder(@RequestBody OrderRequest orderRequest)
            throws IllegalStateException, EntityInvalidException, EmptyBasketException {
        String user = authResolver.getUser();
        Basket basket = basketService.find(user);
        Order order = orderService.createOrder(
                basket,
                orderRequest.getLabel(),
                orderRequest.getOnSuccessUrl(),
                orderSettingsService.getAppSubOrderDuration()
        );
        return new ResponseEntity<>(toResource(OrderDto.fromOrder(order)), HttpStatus.CREATED);
    }

    @ResourceAccess(description = "Retrieve specified order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = GET_ORDER_PATH)
    public ResponseEntity<EntityModel<OrderDto>> retrieveOrder(@PathVariable("orderId") Long orderId) {
        Order order = orderService.loadSimple(orderId);
        if (order != null) {
            return ResponseEntity.ok(toResource(OrderDto.fromOrder(order)));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @ResourceAccess(description = "Resume an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = RESUME_ORDER_PATH)
    public ResponseEntity<Void> resumeOrder(@PathVariable("orderId") Long orderId) throws ModuleException {
        orderService.resume(orderId);
        return ResponseEntity.ok().build();
    }

    @ResourceAccess(description = "Ask for an order to be paused", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = PAUSE_ORDER_PATH)
    public ResponseEntity<Void> pauseOrder(@PathVariable("orderId") Long orderId) throws ModuleException {
        orderService.pause(orderId);
        return ResponseEntity.ok().build();
    }

    @ResourceAccess(description = "Ask for an order to be restarted", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, path = RESTART_ORDER_PATH)
    public ResponseEntity<EntityModel<OrderDto>> restartOrder(@PathVariable("orderId") Long orderId, @RequestBody OrderRequest orderRequest)
            throws ModuleException {
        Order order = orderService.restart(orderId, orderRequest.getLabel(), orderRequest.getOnSuccessUrl());
        return new ResponseEntity<>(toResource(OrderDto.fromOrder(order)), HttpStatus.CREATED);
    }

    @ResourceAccess(description = "Ask for an order to retry files in error", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = RETRY_ORDER_PATH)
    public ResponseEntity<Void> retryOrder(@PathVariable("orderId") Long orderId) throws ModuleException {
        orderService.retryErrors(orderId);
        return ResponseEntity.ok().build();
    }

    @ResourceAccess(description = "Delete an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.DELETE, path = DELETE_ORDER_PATH)
    public ResponseEntity<Void> deleteOrder(@PathVariable("orderId") Long orderId) throws ModuleException {
        orderService.delete(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Remove an order", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.DELETE, path = REMOVE_ORDER_PATH)
    public ResponseEntity<Void> removeOrder(@PathVariable("orderId") Long orderId) throws ModuleException {
        orderService.remove(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Find all specified user orders or all users orders", role = DefaultRole.EXPLOIT)
    @RequestMapping(method = RequestMethod.GET, path = ADMIN_ROOT_PATH)
    public ResponseEntity<PagedModel<EntityModel<OrderDto>>> findAllAdmin(@RequestParam(value = "user", required = false) String user, Pageable pageRequest) {
        Page<Order> orderPage = (Strings.isNullOrEmpty(user)) ? orderService.findAll(pageRequest)
                : orderService.findAll(user, pageRequest);
        return ResponseEntity.ok(toPagedResources(orderPage.map(OrderDto::fromOrder), orderDtoPagedResourcesAssembler));
    }

    @ResourceAccess(description = "Generate a CSV file with all orders", role = DefaultRole.EXPLOIT)
    @RequestMapping(method = RequestMethod.GET, path = ADMIN_ROOT_PATH + CSV, produces = "text/csv")
    public void generateCsv(@RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "from", required = false) String fromParam,
            @RequestParam(name = "to", required = false) String toParam, HttpServletResponse response)
            throws IOException {
        OffsetDateTime from = Strings.isNullOrEmpty(fromParam) ? null : OffsetDateTimeAdapter.parse(fromParam);
        OffsetDateTime to = Strings.isNullOrEmpty(toParam) ? null : OffsetDateTimeAdapter.parse(toParam);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=orders.csv");
        response.setContentType("text/csv");
        orderService.writeAllOrdersInCsv(new BufferedWriter(response.getWriter()), status, from, to);
    }

    @ResourceAccess(description = "Find all user current orders", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = USER_ROOT_PATH)
    public ResponseEntity<PagedModel<EntityModel<OrderDto>>> findAll(Pageable pageRequest) {
        String user = authResolver.getUser();
        return ResponseEntity.ok(toPagedResources(
                orderService.findAll(user, pageRequest, OrderStatus.DELETED, OrderStatus.REMOVED).map(OrderDto::fromOrder),
                orderDtoPagedResourcesAssembler
        ));
    }

    @ResourceAccess(description = "Download a Zip file containing all currently available files", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ZIP_DOWNLOAD_PATH)
    public ResponseEntity<Void> downloadAllAvailableFiles(@PathVariable("orderId") Long orderId, HttpServletResponse response) throws EntityNotFoundException {
        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=order_" + orderId + "_"
                + OffsetDateTime.now().toString().replaceAll(" ", "-") + ".zip");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        List<OrderDataFile> availableFiles = new ArrayList<>(dataFileService.findAllAvailables(orderId));
        // No file available
        if (availableFiles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        try {
            orderDownloadService.downloadOrderCurrentZip(order.getOwner(), availableFiles, response.getOutputStream());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Stream the response
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Download a Metalink file containing all files", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = METALINK_DOWNLOAD_PATH)
    public ResponseEntity<Void> downloadMetalinkFile(@PathVariable("orderId") Long orderId, HttpServletResponse response) throws EntityNotFoundException {
        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }
        return createMetalinkDownloadResponse(order, response);

    }

    @ResourceAccess(description = "Download a metalink file containing all files granted by a token", role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = PUBLIC_METALINK_DOWNLOAD_PATH)
    public ResponseEntity<Void> publicDownloadMetalinkFile(@RequestParam(name = IOrderService.ORDER_TOKEN) String validityToken, HttpServletResponse response)
            throws EntityNotFoundException {
        Long orderId;
        try {
            Jwts.parser().setSigningKey(Encoders.BASE64.encode(secret.getBytes())).parse(validityToken);
            Claims claims = jwtService.parseToken(validityToken, secret);
            orderId = Long.parseLong(claims.get(IOrderService.ORDER_ID_KEY, String.class));
        } catch (InvalidJwtException | MalformedJwtException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }

        return createMetalinkDownloadResponse(order, response);
    }

    /**
     * Fill Response headers and create streaming response
     * @throws EntityNotFoundException
     */
    private ResponseEntity<Void> createMetalinkDownloadResponse(Order order, HttpServletResponse response) {
        String error = null;
        switch (order.getStatus()) {
            case DELETED:
            case REMOVED:
                error = "Order is deleted";
                break;
            case EXPIRED:
                error = "Order is expired since " + order.getExpirationDate().toString();
                break;
            case PENDING:
                error = "Order is not ready yet. Files calculation pending.";
                break;
            case FAILED:
                error = "Order creation failed.";
                break;
            case RUNNING:
            case DONE:
            case DONE_WITH_WARNING:
            case PAUSED:
            default:
                // All those status allow metalink download
                break;
        }
        if (error != null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=order_" + order.getId() + "_" + OffsetDateTime.now() + ".metalink");
            response.setContentType("application/metalink+xml");
            try {
                orderDownloadService.downloadOrderMetalink(order.getId(), response.getOutputStream());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // Stream the response
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    @Override
    public EntityModel<OrderDto> toResource(OrderDto orderDto, Object... extras) {

        EntityModel<OrderDto> resource = resourceService.toResource(orderDto);

        resourceService.addLink(resource, this.getClass(), "retrieveOrder", LinkRels.SELF, MethodParamFactory.build(Long.class, orderDto.getId()));
        resourceService.addLink(resource, this.getClass(), "findAll", LinkRels.LIST, MethodParamFactory.build(Pageable.class));

        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.DOWNLOAD)) {
            resourceService.addLink(resource, this.getClass(), "downloadAllAvailableFiles", LinkRelation.of("download"),
                                    MethodParamFactory.build(Long.class, orderDto.getId()), MethodParamFactory.build(HttpServletResponse.class)
            );
        }
        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.PAUSE)) {
            resourceService.addLink(resource, this.getClass(), "pauseOrder", LinkRelation.of("pause"), MethodParamFactory.build(Long.class, orderDto.getId()));
        }
        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.RESUME)) {
            resourceService.addLink(resource, this.getClass(), "resumeOrder", LinkRelation.of("resume"), MethodParamFactory.build(Long.class, orderDto.getId()));
        }
        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.DELETE)) {
            resourceService.addLink(resource, this.getClass(), "deleteOrder", LinkRels.DELETE, MethodParamFactory.build(Long.class, orderDto.getId()));
        }
        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.REMOVE)) {
            resourceService.addLink(resource, this.getClass(), "removeOrder", LinkRelation.of("remove"), MethodParamFactory.build(Long.class, orderDto.getId()));
        }
        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.RESTART)) {
            resourceService.addLink(resource, this.getClass(), "restartOrder", LinkRelation.of("restart"),
                                    MethodParamFactory.build(Long.class, orderDto.getId()), MethodParamFactory.build(OrderRequest.class));
        }
        if (orderService.isActionAvailable(orderDto.getId(), OrderService.Action.RETRY)) {
            resourceService.addLink(resource, this.getClass(), "retryOrder", LinkRelation.of("retry"), MethodParamFactory.build(Long.class, orderDto.getId()));
        }
        return resource;
    }

}
