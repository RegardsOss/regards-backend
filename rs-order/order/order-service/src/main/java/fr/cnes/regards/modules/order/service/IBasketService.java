package fr.cnes.regards.modules.order.service;

import java.time.OffsetDateTime;

import fr.cnes.regards.framework.module.rest.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.basket.Basket;

/**
 * Basket service
 * @author oroussel
 */
public interface IBasketService {

    /**
     * Create an empty basket
     * @param user user email
     * @return a basket, what else ?
     */
    Basket findOrCreate(String user);

    /**
     * Delete basket
     */
    void deleteIfExists(String user);

    /**
     * Find user basket with all its relations
     * @param email user email
     * @return its basket
     * @throws EmptyBasketException if basket doesn' exist
     */
    Basket find(String email) throws EmptyBasketException;

    /**
     * Load basket with all its relations
      * @param id basket id
     */
    Basket load(Long id);

    /**
     * Add a selection to a basket through an opensearch request. The selection concerns a priori several datasets.
     * Adding a selection concerns RAWDATA and QUICKLOOKS files
     */
    default Basket addSelection(Long basketId, String openSearchRequest) {
        return this.addSelection(basketId, null, openSearchRequest);
    }

    /**
     * Add a selection through an opensearch request. Results are restricted to specified dataset
     * Adding a selection concerns RAWDATA and QUICKLOOKS files by default
     * @param datasetIpId concerned dataset IP_ID (can be null)
     * @param openSearchRequest selection request
     */
    Basket addSelection(Long basketId, String datasetIpId, String openSearchRequest);

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
