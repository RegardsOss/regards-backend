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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.dto.FileSelectionDescriptionDTO;
import fr.cnes.regards.modules.order.domain.exception.CatalogSearchException;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

/**
 * Basket service
 *
 * @author oroussel
 * @author Sébastien Binda
 */
public interface IBasketService {

    /**
     * Create an empty basket
     *
     * @param user user
     * @return a basket, what else ?
     */
    Basket findOrCreate(String user);

    /**
     * Delete basket
     *
     * @param user name used to find the basket to delete
     */
    void deleteIfExists(String user);

    /**
     * Find user basket with all its relations
     *
     * @param user user
     * @return its basket
     * @throws EmptyBasketException if basket doesn' exist
     */
    Basket find(String user) throws EmptyBasketException;

    /**
     * Load basket with all its relations
     *
     * @param id basket id
     * @return {@link Basket}
     */
    Basket load(Long id);

    /**
     * Add a selection to a basket through an opensearch request. The selection concerns a priori several datasets.
     * Adding a selection concerns RAWDATA and QUICKLOOKS files. If a process is associated to a selected dataset, a
     * check is done to verify if the number of items to process is less than the limit defined by the process
     *
     * @param basketId         the identifier of the basket on which the selection will be added
     * @param selectionRequest the selection to add to the basket
     * @return {@link Basket} the basket updated with the selection
     * @throws EmptySelectionException               if there is no files associated to the order
     * @throws TooManyItemsSelectedInBasketException if the number of items selected is greater than the limit
     */
    Basket addSelection(Long basketId, BasketSelectionRequest selectionRequest)
        throws EmptySelectionException, TooManyItemsSelectedInBasketException, CatalogSearchException;

    /**
     * See {@link #addSelection(Long, BasketSelectionRequest)}
     *
     * @param user requesting user
     * @param role user role
     */
    Basket addSelection(Long basketId, BasketSelectionRequest selectionRequest, String user, String role)
        throws EmptySelectionException, TooManyItemsSelectedInBasketException, CatalogSearchException;

    /**
     * Remove specified dataset selection from basket
     *
     * @param basket    basket that contains one or multiple dataset selections
     * @param datasetId identifier used to delete the corresponding dataset selection
     * @return updated {@link Basket}
     */
    Basket removeDatasetSelection(Basket basket, Long datasetId);

    /**
     * Remove specified dated items selection from basket
     *
     * @param basket             basket that contains one or multiple dataset selections regrouped by adding date
     * @param datasetId          id of dataset selection whom items selection belongs to
     * @param itemsSelectionDate the referenced date used to remove the corresponding selection
     * @return updated {@link Basket}
     */
    Basket removeDatedItemsSelection(Basket basket, Long datasetId, OffsetDateTime itemsSelectionDate);

    /**
     * Attach a process uuid to the dataset selection.
     *
     * @param basket      the user's basket
     * @param datasetId   the id of the dataset selection to modify
     * @param description the process UUID and parameters to attach to the dataset selection, remove the existing process desc if null
     * @return update {@link Basket}
     * @throws TooManyItemsSelectedInBasketException if the number of items to process is greater than the limit
     *                                               defined by the process
     */
    Basket attachProcessing(Basket basket, Long datasetId, @Nullable ProcessDatasetDescription description)
        throws TooManyItemsSelectedInBasketException;

    /**
     * Duplicates a basket with the exact same content, but with a new owner
     *
     * @param id    id of the basket to duplicate
     * @param owner owner of the new basket
     * @return the new basket
     */
    Basket duplicate(Long id, String owner);

    /**
     * Transfer ownership of the basket owned by "fromOwner" to "toOwner"
     *
     * @param fromOwner owner of the basket to transfer ownership of
     * @param toOwner   new owner to transfer the basket to
     * @return the modified basket
     */
    Basket transferOwnerShip(String fromOwner, String toOwner);

    /**
     * Update file filter of the dataset selection
     *
     * @param basket                      the user's basket
     * @param dsSelectionId               the id of the dataset selection to modify
     * @param fileSelectionDescriptionDTO the new file filter description
     * @return updated {@link Basket}
     */
    Basket attachFileFilters(Basket basket,
                             Long dsSelectionId,
                             FileSelectionDescriptionDTO fileSelectionDescriptionDTO);
}
