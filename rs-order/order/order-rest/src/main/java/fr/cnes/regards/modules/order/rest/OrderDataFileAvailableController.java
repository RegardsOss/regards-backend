/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.dto.dto.OrderDataFileDTO;
import fr.cnes.regards.modules.order.dto.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.OrderDataFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * @author Thomas GUILLOU
 **/
@RestController
public class OrderDataFileAvailableController implements IResourceController<OrderDataFileDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDataFileAvailableController.class);

    private final IOrderService orderService;

    private final IAuthenticationResolver authResolver;

    private final OrderDataFileService orderDataFileService;

    private final IResourceService resourceService;

    public OrderDataFileAvailableController(IOrderService orderService,
                                            IAuthenticationResolver authResolver,
                                            OrderDataFileService orderDataFileService,
                                            IResourceService resourceService) {
        this.orderService = orderService;
        this.authResolver = authResolver;
        this.orderDataFileService = orderDataFileService;
        this.resourceService = resourceService;
    }

    @Operation(summary = "Get files available in specific order",
               description = "Return files corresponding to the orderId input, if exists and user has access to it")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The order has been successfully retrieved."),
                            @ApiResponse(responseCode = "204",
                                         description = "The order has been successfully retrieved, but no file is available."),
                            @ApiResponse(responseCode = "206",
                                         description = "The order has been successfully retrieved, but is not finished",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "404",
                                         description = "Order not found",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "403",
                                         description = "Order is not accessible for the current user.",
                                         content = { @Content(mediaType = "application/html") }), })
    @ResourceAccess(description = "Retrieve specified order", role = DefaultRole.REGISTERED_USER)
    @GetMapping(path = OrderControllerEndpointConfiguration.FIND_AVAILABLE_FILES_BY_ORDER_PATH)
    public ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> getAvailableFilesOf(
        @PathVariable("orderId") Long orderId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable page,
        @Parameter(hidden = true) PagedResourcesAssembler<OrderDataFileDTO> assembler) {
        Optional<String> owner = orderService.getOrderOwner(orderId);
        if (owner.isPresent()) {
            if (!orderService.hasCurrentUserAccessTo(owner.get())) {
                LOGGER.error("Ordered file is not accessible to current user ({})", authResolver.getUser());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(toPagedResources(orderDataFileService.findAvailableDataFiles(orderId, null, page),
                                                      assembler));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Get files available in specific suborder",
               description = "Return files corresponding to the orderId and filesTaskId input, if exists and user has "
                             + "access to it")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The suborder has been successfully retrieved."),
                            @ApiResponse(responseCode = "204",
                                         description = "The suborder has been successfully retrieved, but no file is available."),
                            @ApiResponse(responseCode = "206",
                                         description = "The suborder has been successfully retrieved, but is not finished",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "404",
                                         description = "Suborder not found",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "403",
                                         description = "Suborder is not accessible for the current user.",
                                         content = { @Content(mediaType = "application/html") }), })
    @ResourceAccess(description = "Retrieve specified suborder", role = DefaultRole.REGISTERED_USER)
    @GetMapping(path = OrderControllerEndpointConfiguration.FIND_AVAILABLE_FILES_BY_SUBORDER_PATH)
    public ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> getAvailableFilesInSuborder(
        @PathVariable("orderId") long orderId,
        @PathVariable("filesTaskId") long filesTaskId,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable page,
        @Parameter(hidden = true) PagedResourcesAssembler<OrderDataFileDTO> assembler) {
        Optional<String> owner = orderService.getOrderOwner(orderId);
        if (owner.isPresent()) {
            if (!orderService.hasCurrentUserAccessTo(owner.get())) {
                LOGGER.error("Ordered file is not accessible to current user ({})", authResolver.getUser());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(toPagedResources(orderDataFileService.findAvailableDataFiles(orderId,
                                                                                                  filesTaskId,
                                                                                                  page), assembler));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    public EntityModel<OrderDataFileDTO> toResource(OrderDataFileDTO dataFile, Object... extras) {
        return resourceService.toResource(dataFile);
    }
}
