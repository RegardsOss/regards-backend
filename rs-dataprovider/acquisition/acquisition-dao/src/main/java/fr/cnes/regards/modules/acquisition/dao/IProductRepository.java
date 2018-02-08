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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;

/**
 * {@link Product} repository
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Repository
public interface IProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph("graph.acquisition.file.complete")
    Product findCompleteById(Long id);

    @EntityGraph("graph.acquisition.file.complete")
    Product findCompleteByProductName(String productName);

    @EntityGraph("graph.acquisition.file.complete")
    Product findCompleteByIpId(String ipId);

    Set<Product> findByState(ProductState state);

    Set<Product> findByProcessingChainAndSipStateAndStateIn(AcquisitionProcessingChain processingChain,
            ProductSIPState sipState, List<ProductState> productStates);

    /**
     * Find {@link ProductState#COMPLETED} or{@link ProductState#FINISHED} products not already scheduled for SIP
     * generation and for the specified acquisition chain.
     * @param processingChain acquisition processing chain
     * @return a set of {@link Product} to schedule
     */
    default Set<Product> findChainProductsToSchedule(AcquisitionProcessingChain processingChain) {
        return findByProcessingChainAndSipStateAndStateIn(processingChain, ProductSIPState.NOT_SCHEDULED,
                                                          Arrays.asList(ProductState.COMPLETED, ProductState.FINISHED));
    }

    /**
     * Find all products according to specified filters
     *
     * @param ingestChain ingest chain
     * @param session session name
     * @param sipState {@link ISipState}
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Page<Product> findByProcessingChainIngestChainAndSessionAndSipState(String ingestChain, String session,
            ISipState sipState, Pageable pageable);

    /**
     * Find all products according to specified filters (no session)
     *
     * @param ingestChain ingest chain
     * @param sipState {@link ISipState}
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Page<Product> findByProcessingChainIngestChainAndSipState(String ingestChain, ISipState sipState,
            Pageable pageable);

    /**
     * Find all products according to specified filters
     *
     * @param ingestChain ingest chain
     * @param session session name
     * @param sipState {@link ISipState}
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Set<Product> findByProcessingChainIngestChainAndSessionAndSipState(String ingestChain, String session,
            ISipState sipState);

    /**
     * Find all products according to specified filters (no session)
     *
     * @param ingestChain ingest chain
     * @param sipState {@link ISipState}
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Set<Product> findByProcessingChainIngestChainAndSipState(String ingestChain, ISipState sipState);

    /**
     * Find {@link Product} by state in transaction with pessimistic read lock
     * @param sipState {@link ISipState}
     * @return a set of products with the above properties
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Set<Product> findWithLockBySipState(ISipState sipState);

    /**
     * Find {@link Product} by state in transaction with pessimistic read lock
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param sipState {@link ISipState}
     * @return a set of products with the above properties
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Set<Product> findWithLockByProcessingChainAndSipState(AcquisitionProcessingChain processingChain,
            ProductSIPState sipState);

    /**
     * Find {@link Product} by state
     * @param sipState {@link ISipState}
     * @return a set of products with the above properties
     */
    Set<Product> findBySipState(ISipState sipState);

    /**
     * Count number of products associated to the given {@link AcquisitionProcessingChain} and in the given state
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param states {@link ProductState}s
     * @return number of matching {@link Product}
     */
    long countByProcessingChainAndStateIn(AcquisitionProcessingChain processingChain, List<ProductState> productStates);

    /**
     * Count number of products of the given {@link AcquisitionProcessingChain} accord to above filters
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param productSipStates {@link ISipState}s
     * @return number of matching {@link Product}
     */
    long countByProcessingChainAndSipStateIn(AcquisitionProcessingChain processingChain,
            List<ISipState> productSipStates);

    /**
     * Count number of {@link Product} associated to the given {@link AcquisitionProcessingChain}
     * @param chain {@link AcquisitionProcessingChain}
     * @return number of {@link Product}
     */
    long countByProcessingChain(AcquisitionProcessingChain chain);
}
