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
package fr.cnes.regards.modules.order.service.request;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.ExceededBasketSizeException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.exception.AutoOrderException;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.settings.OrderSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * This service creates an order automatically by bypassing user interactions.
 *
 * @author Iliana Ghazali
 **/
@Service
public class AutoOrderCompletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoOrderCompletionService.class);

    public static final String ERROR_RESPONSE_FORMAT = "%s: %s"; // SimpleException.class: error cause

    /**
     * Services
     */
    private final BasketService basketService;

    private final IOrderService orderService;

    private final OrderSettingsService orderSettings;

    public AutoOrderCompletionService(BasketService basketService,
                                      IOrderService orderService,
                                      OrderSettingsService orderSettings) {
        this.basketService = basketService;
        this.orderService = orderService;
        this.orderSettings = orderSettings;
    }

    /**
     * Creates an order through a {@link OrderRequestDto}. A {@link Basket} is built from the request and used to
     * create an {@link Order}.
     *
     * @return {@link Order} created in case of success
     * @throws AutoOrderException if the order could not be created
     */
    public Order generateOrder(OrderRequestDto orderRequestDto, String role, boolean checkSizeLimit)
        throws AutoOrderException {
        try {
            Basket basket = basketService.createBasketFromRequest(orderRequestDto, role);
            if (checkSizeLimit) {
                verifyBasketSize(basket, orderRequestDto);
            }
            return orderService.createOrder(basket,
                                            "Generated order " + OffsetDateTimeAdapter.format(OffsetDateTime.now()),
                                            null,
                                            orderSettings.getAppSubOrderDuration(),
                                            orderRequestDto.getUser(),
                                            orderRequestDto.getCorrelationId());
        } catch (ExceededBasketSizeException | TooManyItemsSelectedInBasketException | EmptySelectionException |
                 EntityInvalidException | CatalogSearchException e) {
            String errorMsg = String.format(ERROR_RESPONSE_FORMAT, e.getClass().getSimpleName(), e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new AutoOrderException(errorMsg, e);
        }
    }

    /**
     * Check if basket size does not exceed the maximum size configured in {@link OrderRequestDto#getSizeLimitInBytes()}.
     * If the maximum size is null, no verification will be done.
     *
     * @param basket          basket containing the products requested
     * @param orderRequestDto order request with information to extract
     * @throws ExceededBasketSizeException in case the basket size is exceeded
     */
    private void verifyBasketSize(Basket basket, OrderRequestDto orderRequestDto) throws ExceededBasketSizeException {
        Long maxSizeLimitInBytes = orderRequestDto.getSizeLimitInBytes();
        if (maxSizeLimitInBytes != null) {
            long basketSizeInBytes = basket.getDatasetSelections()
                                           .stream()
                                           .mapToLong(BasketDatasetSelection::getFilesSize)
                                           .sum();
            if (basketSizeInBytes > maxSizeLimitInBytes) {
                throw new ExceededBasketSizeException(String.format(
                    "The size of the basket [%d bytes] exceeds the maximum size allowed [%d bytes]. Please review the"
                    + " order requested so that it does not exceed the maximum size configured.",
                    basketSizeInBytes,
                    maxSizeLimitInBytes));
            } else {
                LOGGER.debug("Size successfully checked: the size of the basket [{} bytes] is below the maximum "
                             + "size allowed [{} bytes]", basketSizeInBytes, maxSizeLimitInBytes);
            }
        }
    }

}
