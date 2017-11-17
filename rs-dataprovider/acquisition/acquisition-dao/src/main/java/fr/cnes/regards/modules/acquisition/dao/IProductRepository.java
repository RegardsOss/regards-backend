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
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;

/**
 * {@link Product} repository
 *
 * @author Christophe Mertz
 */
@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph("graph.acquisition.file.complete")
    Product findCompleteByProductName(String productName);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findBySendedAndStatusIn(Boolean sended, ProductStatus... status);

    @Query("select distinct p.ingestChain from Product p where p.sended=?1 and p.status in ?2")
    Set<String> findDistinctIngestChainBySendedAndStatusIn(Boolean sended, ProductStatus... status);

    @Query("select distinct p.session from Product p where p.ingestChain=?1 and p.sended=?2 and p.status in ?3")
    Set<String> findDistinctSessionByIngestChainAndSendedAndStatusIn(String ingestChain, Boolean sended,
            ProductStatus... status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<Product> findAllByIngestChainAndSessionAndSendedAndStatusIn(String ingestChain, String session, Boolean sended,
            Pageable pageable, ProductStatus... status);

}
