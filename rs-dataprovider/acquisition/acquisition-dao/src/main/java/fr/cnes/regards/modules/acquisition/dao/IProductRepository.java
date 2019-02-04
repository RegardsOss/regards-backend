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
package fr.cnes.regards.modules.acquisition.dao;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
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

    @EntityGraph("graph.product.complete")
    Product findCompleteById(Long id);

    @EntityGraph("graph.product.complete")
    Product findCompleteByProductName(String productName);

    @EntityGraph("graph.product.complete")
    Set<Product> findCompleteByProductNameIn(Collection<String> productNames);

    Page<Product> findByProcessingChainOrderByIdAsc(AcquisitionProcessingChain processingChain, Pageable pageable);

    /**
     * Find all products according to specified filters
     *
     * @param ingestChain ingest chain
     * @param session session name
     * @param sipStates {@link ISipState}s
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Page<Product> findByProcessingChainIngestChainAndSessionAndSipStateIn(String ingestChain, String session,
            Collection<ISipState> sipStates, Pageable pageable);

    /**
     * Find all products according to specified filters (no session)
     *
     * @param ingestChain ingest chain
     * @param sipStates {@link ISipState}s
     * @param pageable page limit
     * @return a page of products with the above properties
     */
    Page<Product> findByProcessingChainIngestChainAndSipStateInOrderByIdAsc(String ingestChain,
            Collection<ISipState> sipStates, Pageable pageable);

    /**
     * Find {@link Product} by state in transaction with pessimistic read lock
     * @param sipState {@link ISipState}
     * @param pageable
     * @return a set of products with the above properties
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<Product> findWithLockBySipStateOrderByIdAsc(ISipState sipState, Pageable pageable);

    /**
     * Find {@link Product} by state in transaction with pessimistic read lock
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param sipState {@link ISipState}
     * @param pageable
     * @return a set of products with the above properties
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<Product> findWithLockByProcessingChainAndSipStateOrderByIdAsc(AcquisitionProcessingChain processingChain,
            ProductSIPState sipState, Pageable pageable);

    Page<Product> findByProcessingChainAndSipStateOrderByIdAsc(AcquisitionProcessingChain processingChain,
            ProductSIPState sipState, Pageable pageable);

    /**
     * Find {@link Product} by state
     * @param sipState {@link ISipState}
     * @param pageable
     * @return a set of products with the above properties
     */
    Page<Product> findBySipStateOrderByIdAsc(ISipState sipState, Pageable pageable);

    /**
     * Count number of products associated to the given {@link AcquisitionProcessingChain} and in the given state
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param productStates
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
     * Count number of generation job that is actually running
     * @param processingChain {@link AcquisitionProcessingChain}
     * @param productSipState {@link ISipState}s as string
     * @return long
     */
    @Query(value = "select count(distinct p.sip_gen_job_info_id) from  {h-schema}t_acquisition_product p where p.processing_chain_id=?1 and p.sip_state=?2",
            nativeQuery = true)
    long countDistinctLastSIPGenerationJobInfoByProcessingChainAndSipState(AcquisitionProcessingChain processingChain,
            String productSipState);

    @Query(value = "select distinct p.lastSIPGenerationJobInfo from  Product p where p.processingChain=?1 and p.sipState=?2")
    Set<JobInfo> findDistinctLastSIPGenerationJobInfoByProcessingChainAndSipStateIn(
            AcquisitionProcessingChain processingChain, ISipState productSipState);

    /**
     * Count number of {@link Product} associated to the given {@link AcquisitionProcessingChain}
     * @param chain {@link AcquisitionProcessingChain}
     * @return number of {@link Product}
     */
    long countByProcessingChain(AcquisitionProcessingChain chain);

    @Modifying
    @Query(value = "UPDATE Product p set p.sipState = ?2 where p.processingChain = ?3 and p.sipState = ?1")
    void updateSipStates(ISipState fromStatus, ISipState toStatus, AcquisitionProcessingChain processingChain);

    @Modifying
    @Query(value = "UPDATE Product p set p.sipState = ?1 where p.productName in (?2)")
    void updateSipStatesByProductNameIn(ISipState state, Collection<String> productNames);

    /**
     * Load a page of product (i.e. with all dependencies).
     */
    default Page<Product> loadAll(Specification<Product> search, Pageable pageable) {
        // as a Specification is used to constrain the page, we cannot simply ask for ids with a query
        // to mimic that, we are querying without any entity graph to extract ids
        Page<Product> products = findAll(search, pageable);
        List<Long> productIds = products.stream().map(p -> p.getId()).collect(Collectors.toList());
        // now that we have the ids, lets load the products and keep the same sort
//        List<Product> loadedProducts = findAllById(productIds, pageable.getSort());
        List<Product> loadedProducts = findAllById(productIds);
        // sort by Id as it is okay with the needs for now.
        loadedProducts.sort(Comparator.comparing(Product::getId));
        return new PageImpl<>(loadedProducts,
                              PageRequest.of(products.getNumber(), products.getSize(), products.getSort()),
                              products.getTotalElements());
    }

    @EntityGraph("graph.product.complete")
    List<Product> findAllById(List<Long> productIds);

    @EntityGraph("graph.product.complete")
    List<Product> findAllById(List<Long> productIds, Sort sort);
}
