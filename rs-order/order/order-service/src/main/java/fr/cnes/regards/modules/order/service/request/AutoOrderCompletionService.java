/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.basket.FileSelectionDescription;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.exception.OrderRequestServiceException;
import fr.cnes.regards.modules.order.service.BasketService;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.order.service.settings.OrderSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;

/**
 * This service creates an order automatically by bypassing user interactions.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class AutoOrderCompletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoOrderCompletionService.class);

    /**
     * Constants
     */

    private static final String SEARCH_ENGINE_TYPE = "legacy";

    private static final String DEFAULT_ACCESS_ROLE = DefaultRole.EXPLOIT.toString();

    public static final String ERROR_RESPONSE_FORMAT = "%s: \"%s\""; // Exception: "error cause"

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
     * @throws OrderRequestServiceException if the order could not be created
     */
    public Order generateOrder(OrderRequestDto orderRequestDto, String role) throws OrderRequestServiceException {
        try {
            Basket basket = createBasketFromRequest(orderRequestDto, role);
            return orderService.createOrder(basket,
                                            "Generated order " + OffsetDateTimeAdapter.format(OffsetDateTime.now()),
                                            null,
                                            orderSettings.getAppSubOrderDuration(),
                                            orderRequestDto.getUser(),
                                            orderRequestDto.getCorrelationId());
        } catch (TooManyItemsSelectedInBasketException | EmptySelectionException | EntityInvalidException e) {
            String errorMsg = String.format(ERROR_RESPONSE_FORMAT, e.getClass().getSimpleName(), e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new OrderRequestServiceException(errorMsg);
        }
    }

    /**
     * Create or update a basket from a {@link OrderRequestDto}. The basket will be completed with :
     * <ul>
     *     <li>{@link BasketDatasetSelection} computed from the request opensearch queries</li>
     *     <li>{@link FileSelectionDescription} built with request filters</li>
     * </ul>
     *
     * @param orderRequestDto order request with information to extract
     * @param role            user role that limits its access to data. Can be null if the request originates from AMQP.
     * @return the updated basket
     * @throws TooManyItemsSelectedInBasketException if there are too many data on the basket
     * @throws EmptySelectionException               if the opensearch queries did not return any data
     */
    private Basket createBasketFromRequest(OrderRequestDto orderRequestDto, String role)
        throws TooManyItemsSelectedInBasketException, EmptySelectionException {
        // create basket
        Basket basket = basketService.findOrCreate(orderRequestDto.getUser());
        // add opensearch query parameters
        for (String query : orderRequestDto.getQueries()) {
            //FIXME: user and role should not be given directly for security reasons.
            // Instead a token must be used, this feature will be released later.
            basket = basketService.addSelection(basket.getId(),
                                                createBasketSelectionRequest(query),
                                                orderRequestDto.getUser(),
                                                role == null ? DEFAULT_ACCESS_ROLE : role);
        }
        // add filters on dataTypes and filenames
        // /!\ to do after addSelection because datasetSelections are init in this method
        if (orderRequestDto.getFilters() != null) {
            basket.getDatasetSelections()
                  .forEach(ds -> ds.setFileSelectionDescription(new FileSelectionDescription(orderRequestDto.getFilters()
                                                                                                            .getDataTypes(),
                                                                                             orderRequestDto.getFilters()
                                                                                                            .getFilenameRegExp())));
        }
        return basket;
    }

    /**
     * Create a {@link BasketSelectionRequest} with a query extracted from {@link OrderRequestDto#getQueries()}
     */
    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest selectionRequest = new BasketSelectionRequest();
        selectionRequest.setEngineType(SEARCH_ENGINE_TYPE);
        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();
        searchParameters.add("q", query);
        selectionRequest.setSearchParameters(searchParameters);
        return selectionRequest;
    }

}
