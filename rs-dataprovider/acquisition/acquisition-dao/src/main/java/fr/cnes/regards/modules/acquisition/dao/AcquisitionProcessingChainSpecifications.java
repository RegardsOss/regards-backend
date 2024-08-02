/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * Specification class to filter DAO searches on {@link AcquisitionProcessingChain} entities.
 *
 * @author Sébastien Binda
 */
public final class AcquisitionProcessingChainSpecifications {

    private static final String LIKE_CHAR = "%";

    private AcquisitionProcessingChainSpecifications() {
    }

    /**
     * Filter on the given attributes (label, locked and mode) and return result ordered by ascending label
     *
     * @param label    {@link String}
     * @param runnable {@link Boolean} search for runnable chains true or false
     * @param mode     {@link AcquisitionProcessingChainMode}
     */
    public static Specification<AcquisitionProcessingChain> search(String label,
                                                                   Boolean runnable,
                                                                   AcquisitionProcessingChainMode mode) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (label != null) {
                predicates.add(cb.like(root.get("label"), LIKE_CHAR + label + LIKE_CHAR));
            }
            if (runnable != null) {
                predicates.add(cb.equal(root.get("locked"), !runnable));
            }
            if (mode != null) {
                predicates.add(cb.equal(root.get("mode"), mode));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
