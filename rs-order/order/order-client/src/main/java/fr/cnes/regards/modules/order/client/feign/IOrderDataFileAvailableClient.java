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
package fr.cnes.regards.modules.order.client.feign;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.order.dto.dto.OrderDataFileDTO;
import fr.cnes.regards.modules.order.dto.OrderControllerEndpointConfiguration;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * REST Client to access available files in suborders
 *
 * @author Iliana Ghazali
 */
@RestClient(name = "rs-order", contextId = "rs-order.order.data.file.available.client")
public interface IOrderDataFileAvailableClient {

    @GetMapping(path = OrderControllerEndpointConfiguration.FIND_AVAILABLE_FILES_BY_SUBORDER_PATH)
    ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> getAvailableFilesInSuborder(
        @PathVariable("orderId") long orderId, @PathVariable("filesTaskId") long filesTaskId, Pageable page);

    @GetMapping(path = OrderControllerEndpointConfiguration.FIND_AVAILABLE_FILES_BY_ORDER_PATH)
    ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> getAvailableFilesInOrder(
        @PathVariable("orderId") long orderId, Pageable page);

}
