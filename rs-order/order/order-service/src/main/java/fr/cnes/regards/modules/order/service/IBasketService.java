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
     * Adding a selection concerned only RAWDATA files
     */
    default void addSelection(Long basketId, String openSearchRequest) {
        this.addSelection(basketId, null, openSearchRequest);
    }

    /**
     * Add a selection through an opensearch request. Results are restricted to specified dataset
     * Adding a selection concerns RAWDATA files by default BUT if a selection already exists fo a dataset,
     * to be able to follow file types selcted for this dataset, all file types are retrieved (RAWDATA and QUICKLOOKS)
     * @param datasetIpId concerned dataset IP_ID (can be null)
     * @param openSearchRequest selection request
     */
    void addSelection(Long basketId, String datasetIpId, String openSearchRequest);

    /**
     * Change data type selection for all items associated to a dataset
     */
    void setFileTypes(Long basketId, String datasetIpId, DataTypeSelection dataTypeSelection);
}
