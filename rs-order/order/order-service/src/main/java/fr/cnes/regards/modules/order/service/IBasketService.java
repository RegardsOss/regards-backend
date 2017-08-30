package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;

/**
 * Basket service
 * @author oroussel
 */
public interface IBasketService {

    /**
     * Create an empty basket
     * @param email user email
     * @return a basket, what else ?
     */
    Basket create(String email);

    /**
     * Find user basket with all its relations
     * @param email user email
     * @return its basket
     */
    Basket find(String email);

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

}
