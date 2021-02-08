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

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
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
import fr.cnes.regards.modules.order.service.IBasketService;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Encoders;

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
    private IOrderDataFileService dataFileService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private PagedResourcesAssembler<OrderDto> orderDtoPagedResourcesAssembler;

    @Value("${regards.order.secret}")
    private String secret;

    @ResourceAccess(description = "Validate current basket and create corresponding order",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, path = USER_ROOT_PATH)
    public ResponseEntity<EntityModel<OrderDto>> createOrder(@RequestBody OrderRequest orderRequest)
            throws IllegalStateException, EntityInvalidException, EmptyBasketException {
        String user = authResolver.getUser();
        Basket basket = basketService.find(user);

        Order order = orderService.createOrder(basket, orderRequest.getLabel(), orderRequest.getOnSuccessUrl());
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

    @ResourceAccess(description = "Remove an order", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.DELETE, path = REMOVE_ORDER_PATH)
    public ResponseEntity<Void> removeOrder(@PathVariable("orderId") Long orderId) throws CannotRemoveOrderException {
        orderService.remove(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Find all specified user orders or all users orders", role = DefaultRole.EXPLOIT)
    @RequestMapping(method = RequestMethod.GET, path = ADMIN_ROOT_PATH)
    public ResponseEntity<PagedModel<EntityModel<OrderDto>>> findAll(
            @RequestParam(value = "user", required = false) String user, Pageable pageRequest) {
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
                                                  orderService.findAll(user, pageRequest, OrderStatus.DELETED,
                                                                       OrderStatus.REMOVED)
                                                          .map(OrderDto::fromOrder),
                                                  orderDtoPagedResourcesAssembler));
    }

    @ResourceAccess(description = "Download a Zip file containing all currently available files",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ZIP_DOWNLOAD_PATH)
    public ResponseEntity<Void> downloadAllAvailableFiles(@PathVariable("orderId") Long orderId,
            HttpServletResponse response) throws EntityNotFoundException {
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
            orderService.downloadOrderCurrentZip(order.getOwner(), availableFiles, response.getOutputStream());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Stream the response
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Download a Metalink file containing all files", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = METALINK_DOWNLOAD_PATH)
    public ResponseEntity<Void> downloadMetalinkFile(@PathVariable("orderId") Long orderId,
            HttpServletResponse response) throws EntityNotFoundException {
        Order order = orderService.loadSimple(orderId);
        if (order == null) {
            throw new EntityNotFoundException(orderId.toString(), Order.class);
        }
        return createMetalinkDownloadResponse(order, response);

    }

    @ResourceAccess(description = "Download a metalink file containing all files granted by a token",
            role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = PUBLIC_METALINK_DOWNLOAD_PATH)
    public ResponseEntity<Void> publicDownloadMetalinkFile(
            @RequestParam(name = IOrderService.ORDER_TOKEN) String validityToken, HttpServletResponse response)
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
    private ResponseEntity<Void> createMetalinkDownloadResponse(Order order, HttpServletResponse response)
            throws EntityNotFoundException {
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
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=order_" + order.getId() + "_"
                    + OffsetDateTime.now().toString() + ".metalink");
            response.setContentType("application/metalink+xml");
            try {
                orderService.downloadOrderMetalink(order.getId(), response.getOutputStream());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // Stream the response
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    @Override
    public EntityModel<OrderDto> toResource(OrderDto order, Object... extras) {
        return resourceService.toResource(order);
    }
}
