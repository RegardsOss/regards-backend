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
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.exception.*;
import fr.cnes.regards.modules.order.dto.dto.BasketDto;
import fr.cnes.regards.modules.order.dto.dto.BasketSelectionRequest;
import fr.cnes.regards.modules.order.dto.dto.FileSelectionDescriptionDTO;
import fr.cnes.regards.modules.order.dto.dto.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.IBasketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;

/**
 * Basket controller
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(BasketController.ORDER_BASKET)
public class BasketController implements IResourceController<BasketDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasketController.class);

    public static final String SELECTION = "/selection";

    public static final String DATASET_DATASET_SELECTION_ID = "/dataset/{datasetSelectionId}";

    public static final String DATASET_DATASET_SELECTION_ID_UPDATE_PROCESS = DATASET_DATASET_SELECTION_ID
                                                                             + "/updateProcessing";

    public static final String DATASET_DATASET_SELECTION_ID_ITEMS_SELECTION_DATE = "/dataset/{datasetSelectionId}/{itemsSelectionDate}";

    public static final String ORDER_BASKET = "/order/basket";

    public static final String DATASET_DATASET_SELECTION_ID_UPDATE_FILE_FILTERS = "/dataset/{datasetSelectionId}/updateFileFilters";

    public static final String INSERT_PRODUCTS_ENDPOINT = "/upload";

    private final IResourceService resourceService;

    private final IBasketService basketService;

    private final IAuthenticationResolver authResolver;

    public BasketController(IResourceService resourceService,
                            IBasketService basketService,
                            IAuthenticationResolver authResolver) {
        this.resourceService = resourceService;
        this.basketService = basketService;
        this.authResolver = authResolver;
    }

    /**
     * Add a selection to the basket
     *
     * @param basketSelectionRequest selection request
     * @return updated or created basket
     */
    @ResourceAccess(description = "Add a selection to the basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.POST, value = SELECTION)
    @Operation(summary = "Add a selection to the basket of current user")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Product correctly added"),
                            @ApiResponse(responseCode = "204", description = "Zero product has been added"),
                            @ApiResponse(responseCode = "422",
                                         description = "Error while elastic searching",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "409",
                                         description = "Number of selected products in the basket exceeds the maximum number allowed",
                                         content = { @Content(mediaType = "application/json") }) })
    public ResponseEntity<EntityModel<BasketDto>> addSelection(
        @Valid @RequestBody BasketSelectionRequest basketSelectionRequest)
        throws TooManyItemsSelectedInBasketException, EmptySelectionException, CatalogSearchException {

        Basket basket = basketService.findOrCreate(authResolver.getUser());

        basket = basketService.addSelection(basket.getId(), basketSelectionRequest);

        return ResponseEntity.ok(toResource(basket.toBasketDto()));
    }

    /**
     * Add a selection to the basket
     *
     * @param file a file where each line is a providerId of an object to add.
     * @return updated or created basket
     */
    @Operation(summary = "Add a selection to the basket of current user from a file",
               description = "Add a selection to the basket of current user from a file,"
                             + " which contains a product identifier per line.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Product correctly added"),
                            @ApiResponse(responseCode = "204", description = "Zero product has been added"),
                            @ApiResponse(responseCode = "422",
                                         description = "Error while elastic searching",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "409",
                                         description = "Number of selected products in the basket exceeds the maximum number allowed",
                                         content = { @Content(mediaType = "application/json") }),
                            @ApiResponse(responseCode = "413",
                                         description = "Too many products in payload",
                                         content = { @Content(mediaType = "application/json") }) })
    @ResourceAccess(description = "Add a selection to the basket from a file", role = DefaultRole.REGISTERED_USER)
    @PostMapping(value = SELECTION + INSERT_PRODUCTS_ENDPOINT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityModel<BasketDto>> addSelectionFromFile(@RequestParam("file") MultipartFile file)
        throws TooManyItemsInFileException, CatalogSearchException, EmptySelectionException,
        TooManyItemsSelectedInBasketException {
        return ResponseEntity.ok(toResource(basketService.addSelectionFromFile(file).toBasketDto()));
    }

    /**
     * Attach or remove a process description to the given dataset selection.
     *
     * @param dsSelectionId dataset selection id (from basket)
     * @param description   the optional description of the process; remove process desc if null
     * @return updated basket
     * @throws EmptyBasketException if no basket currently exists
     */
    @ResourceAccess(description = "Attach process description to dataset selection from basket",
                    role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.PUT, path = DATASET_DATASET_SELECTION_ID_UPDATE_PROCESS)
    public ResponseEntity<EntityModel<BasketDto>> attachProcessDescriptionToDatasetSelection(
        @PathVariable("datasetSelectionId") Long dsSelectionId,
        @RequestBody(required = false) ProcessDatasetDescription description)
        throws EmptyBasketException, TooManyItemsSelectedInBasketException {
        Basket basket = basketService.find(authResolver.getUser());
        if (basket.getDatasetSelections().stream().anyMatch(ds -> ds.getFileSelectionDescription() != null)) {
            LOGGER.error("Cannot set processing if any file filter exists.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Basket modified = basketService.attachProcessing(basket, dsSelectionId, description);
        return ResponseEntity.ok(toResource(modified.toBasketDto()));
    }

    /**
     * Remove dataset selection from basket
     *
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
        BasketDto dto = basket.toBasketDto();
        return ResponseEntity.ok(toResource(dto));
    }

    /**
     * Remove dated item selection under dataset selection from basket
     *
     * @param dsSelectionId         dataset selection id (from basket, the one which contains dated items selection)
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
        OffsetDateTime itemsSelectionDate = OffsetDateTimeAdapter.parse(URLDecoder.decode(itemsSelectionDateStr,
                                                                                          Charset.defaultCharset()));
        Basket basket = basketService.find(authResolver.getUser());
        basket = basketService.removeDatedItemsSelection(basket, dsSelectionId, itemsSelectionDate);
        BasketDto dto = basket.toBasketDto();
        return ResponseEntity.ok(toResource(dto));
    }

    /**
     * Retrieve basket if it does exist
     *
     * @return basket
     */
    @ResourceAccess(description = "Get the basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<EntityModel<BasketDto>> get() {
        try {
            Basket basket = basketService.find(authResolver.getUser());
            BasketDto dto = basket.toBasketDto();
            return ResponseEntity.ok(toResource(dto));
        } catch (EmptyBasketException e) {
            // This is a normal case, no log needed
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * Empty current basket
     *
     * @return nothing
     */
    @ResourceAccess(description = "Empty the basket", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> empty() {
        basketService.deleteIfExists(authResolver.getUser());
        return ResponseEntity.noContent().build();
    }

    @ResourceAccess(description = "Update file filters applied on specific dataset", role = DefaultRole.REGISTERED_USER)
    @PutMapping(DATASET_DATASET_SELECTION_ID_UPDATE_FILE_FILTERS)
    public ResponseEntity<EntityModel<BasketDto>> updateFileFilters(
        @PathVariable("datasetSelectionId") Long dsSelectionId,
        @RequestBody(required = false) FileSelectionDescriptionDTO fileSelectionDescriptionDTO)
        throws EmptyBasketException {
        Basket basket = basketService.find(authResolver.getUser());
        if (basket.getDatasetSelections().stream().anyMatch(BasketDatasetSelection::hasProcessing)) {
            LOGGER.error("Cannot set file filter if any processing is attached.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        basket = basketService.attachFileFilters(basket, dsSelectionId, fileSelectionDescriptionDTO);
        return ResponseEntity.ok(toResource(basket.toBasketDto()));
    }

    @Override
    public EntityModel<BasketDto> toResource(BasketDto basket, Object... extras) {
        return resourceService.toResource(basket);
    }
}
