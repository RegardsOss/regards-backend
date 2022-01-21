/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.manager.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.session.manager.domain.ManagerState;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import java.util.Set;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications to filter DAO search on {@link Source}
 *
 * @author Iliana Ghazali
 **/
public class SourceManagerSpecifications {

    private SourceManagerSpecifications(){
    }

    public static Specification<Source> search(String name, String state) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if(name != null) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }

            if(state !=null) {
                Path<ManagerState> managerState = root.get("managerState");
                if(state.equals("errors") || state.equals("waiting") || state.equals("running")) {
                    predicates.add(cb.isTrue(managerState.get(state)));
                } else if(state.equals("ok")) {
                    predicates.add(cb.isFalse(managerState.get("errors")));
                    predicates.add(cb.isFalse(managerState.get("waiting")));
                    predicates.add(cb.isFalse(managerState.get("running")));
                }
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}