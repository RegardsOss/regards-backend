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

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Specification class to filter DAO searches on {@link Product} entities.
 * @author Sébastien Binda
 */
public class ProductSpecifications {

    private static final String LIKE_CHAR = "%";

    private ProductSpecifications() {
    }

    /**
     * Filter on the given attributes (sessionId, owner, ingestDate and state) and return result ordered by descending ingestDate
     * @param state {@link ProductState}
     * @param sipState {@link SIPState}
     * @param productName {@link String}
     * @param session {@link String}
     * @param processingChainId {@likn Long} id of {@link AcquisitionProcessingChain}
     * @param from {@link OffsetDateTime}
     * @return {@link Specification}<{@link Product}>
     */
    public static Specification<Product> search(ProductState state, SIPState sipState, String productName,
            String session, Long processingChainId, OffsetDateTime from) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (productName != null) {
                predicates.add(cb.like(root.get("productName"), LIKE_CHAR + productName + LIKE_CHAR));
            }
            if (session != null) {
                predicates.add(cb.like(root.get("session"), LIKE_CHAR + session + LIKE_CHAR));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (processingChainId != null) {
                AcquisitionProcessingChain chain = new AcquisitionProcessingChain();
                chain.setId(processingChainId);
                predicates.add(cb.equal(root.get("processingChain"), chain));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdate"), from));
            }
            if (sipState != null) {
                predicates.add(cb.equal(root.get("sipState"), sipState));
            }
            query.orderBy(cb.desc(root.get("lastUpdate")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
