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
package fr.cnes.regards.modules.ingest.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.Set;

/**
 * JPA {@link Specification} to define {@link Predicate}s for criteria search for {@link IngestRequest} from repository.
 *
 * @author Léo Mieulet
 */
public final class IngestRequestSpecifications {

    private IngestRequestSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<IngestRequest> searchByRemoteStepId(String remoteStepGroupId) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Path<Object> attributeRequeted = root.get("remoteStepGroupIds");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequeted,
                                                                                           Lists.newArrayList(
                                                                                               remoteStepGroupId),
                                                                                           cb));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    public static Specification<IngestRequest> searchByRemoteStepIds(List<String> remoteStepGroupIds) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Path<Object> attributeRequeted = root.get("remoteStepGroupIds");
            predicates.add(SpecificationUtils.buildPredicateIsJsonbArrayContainingElements(attributeRequeted,
                                                                                           remoteStepGroupIds,
                                                                                           cb));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
