/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import java.util.Set;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Léo Mieulet
 */
public final class AbstractRequestSpecifications {

    private AbstractRequestSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<AbstractRequest> searchAllByRemoteStepGroupId(String groupId) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();

            if (groupId != null) {
                Path<Object> attributeRequested = root.get("remoteStepGroupIds");
                predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequested, Lists.newArrayList(groupId), cb));
            }

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
