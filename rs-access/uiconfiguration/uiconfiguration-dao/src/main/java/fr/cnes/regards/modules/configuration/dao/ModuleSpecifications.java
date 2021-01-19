/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.dao;

import java.util.Set;

import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.configuration.domain.Module;

/**
 * JPA {@link Specification} to define {@link Predicate}s for criteria search for {@link Module} from repository.
 * @author Sébastien Binda
 */
public class ModuleSpecifications {

    private ModuleSpecifications() {
    }

    /**
     * Filter on the given attributes to find {@link Module}s
     * @param applicationId {@link String}
     * @param active {@link Boolean}
     * @param type {@link String}
     * @return {@link Module} {@link Specification}
     */
    public static Specification<Module> search(String applicationId, Boolean active, String type) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (applicationId != null) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            query.orderBy(cb.desc(root.get("type")), cb.desc(root.get("description")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
