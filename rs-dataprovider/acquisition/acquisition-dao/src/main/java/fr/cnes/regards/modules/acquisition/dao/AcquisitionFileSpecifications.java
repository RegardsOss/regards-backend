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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Specification class to filter DAO searches on {@link AcquisitionFile} entities.
 * @author Sébastien Binda
 */
public class AcquisitionFileSpecifications {

    private static final String LIKE_CHAR = "%";

    private AcquisitionFileSpecifications() {
    }

    /**
     * Filter on the given attributes and return result ordered by descending ingestDate
     * * @param filePath {@link String}
     * @param states {@link AcquisitionFileState}
     * @param productId {@link Long} identifier of {@link Product}
     * @param from {@link OffsetDateTime}
     * @return @return {@link Specification}<{@link AcquisitionFile}>
     */
    public static Specification<AcquisitionFile> search(String filePath, List<AcquisitionFileState> states,
            Long productId, Long chainId, OffsetDateTime from) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            if (chainId != null) {
                Root<AcquisitionProcessingChain> chainRoot = query.from(AcquisitionProcessingChain.class);
                predicates.add(cb.equal(chainRoot.join("fileInfos").get("id"), root.get("fileInfo").get("id")));
                predicates.add(cb.equal(chainRoot.get("id"), chainId));
            }

            if (filePath != null) {
                predicates.add(cb.like(root.get("filePath").as(String.class), LIKE_CHAR + filePath + LIKE_CHAR));
            }
            if (states != null && !states.isEmpty()) {
                Set<Predicate> statePredicates = Sets.newHashSet();
                for (AcquisitionFileState state : states) {
                    statePredicates.add(cb.equal(root.get("state"), state));
                }
                predicates.add(cb.or(statePredicates.toArray(new Predicate[statePredicates.size()])));
            }
            if (productId != null) {
                Product product = new Product();
                product.setId(productId);
                predicates.add(cb.equal(root.get("product"), product));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("acqDate"), from));
            }
            query.orderBy(cb.desc(root.get("acqDate")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
