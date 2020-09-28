/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.UUID;

import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;

/**
 * Basket service
 * @author oroussel
 * @author Sébastien Binda
 */
public interface IBasketService {

    /**
     * Create an empty basket
     * @param user user
     * @return a basket, what else ?
     */
    Basket findOrCreate(String user);

    /**
     * Delete basket
     * @param user
     */
    void deleteIfExists(String user);

    /**
     * Find user basket with all its relations
     * @param user user
     * @return its basket
     * @throws EmptyBasketException if basket doesn' exist
     */
    Basket find(String user) throws EmptyBasketException;

    /**
     * Load basket with all its relations
      * @param id basket id
     * @return {@link Basket}
     */
    Basket load(Long id);

    /**
     * Add a selection to a basket through an opensearch request. The selection concerns a priori several datasets.
     * Adding a selection concerns RAWDATA and QUICKLOOKS files
     * @param basketId
     * @param selectionRequest
     * @return {@link Basket}
     * @throws EmptySelectionException
     */
    Basket addSelection(Long basketId, BasketSelectionRequest selectionRequest) throws EmptySelectionException;

    /**
     * Remove specified dataset selection from basket
     * @param basket
     * @param datasetId
     * @return updated {@link Basket}
     */
    Basket removeDatasetSelection(Basket basket, Long datasetId);

    /**
     * Remove specified dated items selection from basket
     * @param basket
     * @param datasetId id of dataset selection whom items selection belongs to
     * @param itemsSelectionDate
     * @return updated {@link Basket}
     */
    Basket removeDatedItemsSelection(Basket basket, Long datasetId, OffsetDateTime itemsSelectionDate);

    /**
     * Attach a process uuid to the dataset selection
     * @param basket the user's basket
     * @param datasetId the id of the dataset selection to modify
     * @param processUuid the process UUID to attach to the dataset selection
     * @return update {@link Basket}
     */
    Basket attachProcessing(Basket basket, Long datasetId, UUID processUuid);

}
