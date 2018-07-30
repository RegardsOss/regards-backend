/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.order.domain.basket.Basket;
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
     */
    Basket load(Long id);

    /**
     * Add a selection to a basket through an opensearch request. The selection concerns a priori several datasets.
     * Adding a selection concerns RAWDATA and QUICKLOOKS files
     */
    Basket addSelection(Long basketId, BasketSelectionRequest selectionRequest) throws EmptySelectionException;

    /**
     * Remove specified dataset selection from basket
     * @return updated basket
     */
    Basket removeDatasetSelection(Basket basket, Long datasetId);

    /**
     * Remove specified dated items selection from basket
     * @param datasetId id of dataset selection whom items selection belongs to
     * @return updated basket
     */
    Basket removeDatedItemsSelection(Basket basket, Long datasetId, OffsetDateTime itemsSelectionDate);
}
