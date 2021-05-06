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
package fr.cnes.regards.framework.modules.session.management.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.session.management.domain.ManagerState;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import java.util.Set;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications for {@link Session}
 *
 * @author Iliana Ghazali
 **/
public class SessionManagerSpecifications {

    private static final String LIKE_CHAR = "%";

    private SessionManagerSpecifications() {
    }

    public static Specification<Session> search(String name, String state, String source) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (name != null) {
                predicates.add(cb.like(root.get("name"), LIKE_CHAR + name + LIKE_CHAR));
            }

            if (source != null) {
                predicates.add(cb.like(root.get("source"), LIKE_CHAR + source + LIKE_CHAR));
            }

            if (state != null) {
                Path<ManagerState> managerState = root.get("managerState");
                if (state.equals("errors") || state.equals("waiting") || state.equals("running")) {
                    predicates.add(cb.isTrue(managerState.get(state)));
                } else if (state.equals("ok")) {
                    predicates.add(cb.isFalse(managerState.get("errors")));
                    predicates.add(cb.isFalse(managerState.get("waiting")));
                    predicates.add(cb.isFalse(managerState.get("running")));
                }
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
