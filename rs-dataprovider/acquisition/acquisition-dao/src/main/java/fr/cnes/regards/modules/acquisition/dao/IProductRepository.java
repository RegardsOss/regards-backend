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
package fr.cnes.regards.modules.acquisition.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;

/**
 * {@link Product} repository
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph("graph.metaproduct.complete")
    Product findCompleteByProductName(String productName);

    @EntityGraph("graph.metaproduct.complete")
    Product findCompleteByIpId(String ipId);

    Set<Product> findByStatus(ProductState status);

    @Query(value = "select p.* from {h-schema}t_acquisition_product p, {h-schema}t_acquisition_meta_product mp, {h-schema}t_acquisition_chain apc where p.sip_state=?1 and p.product_state in ?2 and p.meta_product_id=mp.id and mp.id=apc.meta_product_id and apc.label=?3",
            nativeQuery = true)
    Set<Product> findChainProducts(ProductSIPState sipState, List<ProductState> productStates, String chainLabel);

    /**
     * Find {@link ProductState#COMPLETED} or{@link ProductState#FINISHED} products not already scheduled for SIP
     * generation and for the specified acquisition chain.
     * @param chainLabel acquisition chain label
     * @return a set of {@link Product} to schedule
     */
    default Set<Product> findChainProductsToSchedule(String chainLabel) {
        return findChainProducts(ProductSIPState.NOT_SCHEDULED,
                                 Arrays.asList(ProductState.COMPLETED, ProductState.FINISHED), chainLabel);
    }

    /**
     * @param ingestChain ingest processing chain name
     * @param session session name
     * @param sipState {@link ISipState}
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Page<Product> findByIngestChainAndSessionAndSipState(String ingestChain, String session, ISipState sipState,
            Pageable pageable);

    /**
     * @param ingestChain ingest processing chain name
     * @param session session name
     * @param sipState {@link ISipState}
     * @param pageable page limit
     * @return all products with the above properties
     */
    Set<Product> findByIngestChainAndSessionAndSipState(String ingestChain, String session, ISipState sipState);

    /**
     * @param sipState {@link ISipState}
     * @return a set of products with the above properties
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Set<Product> findBySipState(ISipState sipState);
}
