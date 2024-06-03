/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.config.mock;

import fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils;
import fr.cnes.regards.modules.order.client.feign.IOrderDataFileAvailableClient;
import fr.cnes.regards.modules.order.dto.dto.OrderDataFileDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Bean to mock rs-order {@link IOrderDataFileAvailableClient} behaviour.
 *
 * @author Iliana Ghazali
 **/
public class OrderDataFileAvailableClientMock implements IOrderDataFileAvailableClient {

    @Override
    public ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> getAvailableFilesInSuborder(long orderId,
                                                                                                 long filesTaskId,
                                                                                                 Pageable page) {
        return null;
    }

    @Override
    public ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> getAvailableFilesInOrder(long orderId,
                                                                                              Pageable page) {
        List<OrderDataFileDTO> simulatedOrderDataFiles = DeliveryStepUtils.buildOrderDataFileDtos();
        return ResponseEntity.ok(DeliveryStepUtils.handleOrderDataFilesDtosByPage(page.getPageNumber(),
                                                                                  page.getPageSize(),
                                                                                  simulatedOrderDataFiles));
    }
}
