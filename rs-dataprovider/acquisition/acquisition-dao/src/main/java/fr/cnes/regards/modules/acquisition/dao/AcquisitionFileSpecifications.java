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

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;

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
     * @param state {@link AcquisitionFileState}
     * @param productId {@link Long} identifier of {@link Product}
     * @param from {@link OffsetDateTime}
     * @return @return {@link Specification}<{@link AcquisitionFile}>
     */
    public static Specification<AcquisitionFile> search(String filePath, AcquisitionFileState state, Long productId,
            OffsetDateTime from) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (filePath != null) {
                predicates.add(cb.like(root.get("filePath"), LIKE_CHAR + filePath + LIKE_CHAR));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
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
