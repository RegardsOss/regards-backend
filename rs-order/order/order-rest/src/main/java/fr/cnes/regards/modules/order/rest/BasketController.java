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
package fr.cnes.regards.modules.order.rest;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.dto.BasketDto;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.IBasketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;

/**
 * Basket controller
 * @author oroussel
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(BasketController.ORDER_BASKET)
public class BasketController implements IResourceController<BasketDto> {

    public static final String SELECTION = "/selection";

    public static final String DATASET_DATASET_SELECTION_ID = "/dataset/{datasetSelectionId}";
    public static final String DATASET_DATASET_SELECTION_ID_UPDATE_PROCESS = DATASET_DATASET_SELECTION_ID + "/updateProcessing";

    public static final String DATASET_DATASET_SELECTION_ID_ITEMS_SELECTION_DATE = "/dataset/{datasetSelectionId}/{itemsSelectionDate}";

    public static final String ORDER_BASKET = "/order/basket";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IBasketService basketService;

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Add a selection to the basket
     * @param basketSelectionRequest selection request
     * @return updated or created basket
     */
    @ResourceAccess(description = "Add a selection to the basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, value = SELECTION)
    public ResponseEntity<EntityModel<BasketDto>> addSelection(
            @Valid @RequestBody BasketSelectionRequest basketSelectionRequest) throws EmptySelectionException, TooManyItemsSelectedInBasketException {
        String user = authResolver.getUser();
        Basket basket = basketService.findOrCreate(user);
        basket = basketService.addSelection(basket.getId(), basketSelectionRequest);
        BasketDto dto = BasketDto.makeBasketDto(basket);
        return ResponseEntity.ok(toResource(dto));
    }

    /**
     * Attach or remove a process description to the given dataset selection.
     * @param dsSelectionId dataset selection id (from basket)
     * @param description the optional description of the process; remove process desc if null
     * @return updated basket
     * @throws EmptyBasketException if no basket currently exists
     */
    @ResourceAccess(description = "Attach process description to dataset selection from basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = DATASET_DATASET_SELECTION_ID_UPDATE_PROCESS)
    public ResponseEntity<EntityModel<BasketDto>> attachProcessDescriptionToDatasetSelection(
            @PathVariable("datasetSelectionId") Long dsSelectionId,
            @RequestBody(required = false) ProcessDatasetDescription description
    ) throws EmptyBasketException, TooManyItemsSelectedInBasketException {
        Basket basket = basketService.find(authResolver.getUser());
        Basket modified = basketService.attachProcessing(basket, dsSelectionId, description);
        BasketDto dto = BasketDto.makeBasketDto(modified);
        return ResponseEntity.ok(toResource(dto));
    }

    /**
     * Remove dataset selection from basket
     * @param dsSelectionId dataset selection id (from basket)
     * @return updated basket
     * @throws EmptyBasketException if no basket currently exists
     */
    @ResourceAccess(description = "Remove dataset selection from basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.DELETE, value = DATASET_DATASET_SELECTION_ID)
    public ResponseEntity<EntityModel<BasketDto>> removeDatasetSelection(
            @PathVariable("datasetSelectionId") Long dsSelectionId) throws EmptyBasketException {
        Basket basket = basketService.find(authResolver.getUser());
        basket = basketService.removeDatasetSelection(basket, dsSelectionId);
        BasketDto dto = BasketDto.makeBasketDto(basket);
        return ResponseEntity.ok(toResource(dto));
    }

    /**
     * Remove dated item selection under dataset selection from basket
     * @param dsSelectionId dataset selection id (from basket, the one which contains dated items selection)
     * @param itemsSelectionDateStr items selection date
     * @return updated basket
     * @throws EmptyBasketException if no basket currently exists
     */
    @ResourceAccess(description = "Remove dated item selection under dataset selection from basket",
            role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.DELETE, value = DATASET_DATASET_SELECTION_ID_ITEMS_SELECTION_DATE)
    public ResponseEntity<EntityModel<BasketDto>> removeDatedItemsSelection(
            @PathVariable("datasetSelectionId") Long dsSelectionId,
            @PathVariable("itemsSelectionDate") String itemsSelectionDateStr) throws EmptyBasketException {
        try {
            OffsetDateTime itemsSelectionDate = OffsetDateTimeAdapter
                    .parse(URLDecoder.decode(itemsSelectionDateStr, Charset.defaultCharset().toString()));
            Basket basket = basketService.find(authResolver.getUser());
            basket = basketService.removeDatedItemsSelection(basket, dsSelectionId, itemsSelectionDate);
            BasketDto dto = BasketDto.makeBasketDto(basket);
            return ResponseEntity.ok(toResource(dto));
        } catch (UnsupportedEncodingException e) {
            throw new RsRuntimeException(e);
        }
    }

    /**
     * Retrieve basket if it does exist
     * @return basket
     */
    @ResourceAccess(description = "Get the basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<EntityModel<BasketDto>> get() {
        try {
            Basket basket = basketService.find(authResolver.getUser());
            BasketDto dto = BasketDto.makeBasketDto(basket);
            return ResponseEntity.ok(toResource(dto));
        } catch (EmptyBasketException e) {
            // This is a normal case, no log needed
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * Empty current basket
     * @return nothing
     */
    @ResourceAccess(description = "Empty the basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> empty() {
        basketService.deleteIfExists(authResolver.getUser());
        return ResponseEntity.noContent().build();
    }

    @Override
    public EntityModel<BasketDto> toResource(BasketDto basket, Object... extras) {
        return resourceService.toResource(basket);
    }
}
