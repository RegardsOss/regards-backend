/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * 
 * @author Christophe Mertz
 * 
 */
public interface IProductService {

    Product save(Product product);

    /**
     * @return all {@link Product}
     */
    Page<Product> retrieveAll(Pageable page);

    /**
     * Retrieve one specified {@link Product}
     * @param id {@link Product}
     */
    Product retrieve(Long id);

    /**
     * Retrieve one specified {@link Product}
     * @param productName a product name
     */
    Product retrieve(String productName);

    /**
     * Delete one specified {@link Product}
     * @param id {@link Product}
     */
    void delete(Long id);

    /**
     * Delete one specified {@link Product}
     * @param product {@link Product} to delete
     */
    void delete(Product product);

    Set<Product> findByStatus(ProductStatus status);

    Set<Product> findBySendedAndStatusIn(Boolean sended, ProductStatus... status);

    Set<String> findDistinctIngestChainBySendedAndStatusIn(Boolean sended, ProductStatus... status);

    Set<String> findDistinctSessionByIngestChainAndSendedAndStatusIn(String ingestChain, Boolean sended,
            ProductStatus... status);

    Page<Product> findAllByIngestChainAndSessionAndSendedAndStatusIn(String ingestChain, String session, Boolean sended,
            Pageable pageable, ProductStatus... status);

    /**
     * Calcul the {@link ProductStatus} :
     * 
     * <li>{@link ProductStatus#ACQUIRING} : the initial state, at least a mandatory file is missing</br></br>
     * 
     * <li>{@link ProductStatus#COMPLETED} : all mandatory files is acquired</br></br>
     * 
     * <li>{@link ProductStatus#FINISHED} : the mandatory and optional files are acquired</br></br>
     * 
     * <li>{@link ProductStatus#SAVED} : the {@link Product} is saved by the microservice Ingest</br></br>
     * 
     * <li>{@link ProductStatus#ERROR} : the {@link Product} is in error</br></br>
     * 
     * @param product the {@link Product}
     */
    void calcProductStatus(Product product);

    /**
     * Get the {@link Product} corresponding to the productName and calculate the {@link ProductStatus}.<br>
     * If it does not exists, create this {@link Product}.
     * 
     * @param session the current session
     * @param acqFile the {@link AcquisitionFile} to add to the {@link Product}
     * @param productName the {@link Product} name
     * @param metaProduct the {@link MetaProduct} of the {@link Product}
     * @param ingestChain the current ingest processing chain
     * @return the existing {@link Product} corresponding to the product name
     */
    Product linkAcquisitionFileToProduct(String session, AcquisitionFile acqFile, String productName,
            MetaProduct metaProduct, String ingestChain);

    /**
     * Set the {@link SIP} to the {@link Product} and ppersist it.
     * @param product the current {@link Product}
     * @param sip the {@link SIP}
     */
    void setSipAndSave(Product product, SIP sip);
    
//    /**
//     * Mark the {@link Product} as send to ingest and persists it.
//     * @param sipId the {@link Product} identifier
//     */
//    void setProductAsSend(String sipId);
//    
//    void setStatusAndSaved(String sipId, ProductStatus status);


}
