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
package fr.cnes.regards.modules.accessrights.dao.projects;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Stephane Cortine
 */
public class ProjectUserSpecificationsBuilder
    extends AbstractSpecificationsBuilder<ProjectUser, SearchProjectUserParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(likeIgnoreCase("email", parameters.getEmail()));

            specifications.add(likeIgnoreCase("lastName", parameters.getLastName()));

            specifications.add(likeIgnoreCase("firstName", parameters.getFirstName()));

            specifications.add(useValuesRestriction("status", parameters.getStatus()));

            specifications.add(useValuesRestriction("origin", parameters.getOrigins()));

            specifications.add(useValuesRestrictionJoined("role", "name", parameters.getRoles()));

            specifications.add(before("created", parameters.getCreationDate().getBefore()));
            specifications.add(after("created", parameters.getCreationDate().getAfter()));

            specifications.add(before("lastConnection", parameters.getLastConnection().getBefore()));
            specifications.add(after("lastConnection", parameters.getLastConnection().getAfter()));

            specifications.add(useValuesRestrictionJoinSet("accessGroups", parameters.getAccessGroups()));

            specifications.add(hasRemainingQuotaBelow(parameters.getQuotaWarningCount()));
        }
    }

    private Specification<ProjectUser> hasRemainingQuotaBelow(Long value) {
        if (value == null || value < 0L) {
            return null;
        } else {
            return (root, query, criteriaBuilder) -> {
                Long minQuota = -1L;
                Path<Long> maxQuota = root.get("maxQuota");
                Path<Long> currentQuota = root.get("currentQuota");

                Predicate notNullPredicate = criteriaBuilder.and(criteriaBuilder.isNotNull(maxQuota),
                                                                 criteriaBuilder.isNotNull(currentQuota));
                Predicate limitedQuotaPredicate = criteriaBuilder.greaterThan(maxQuota, minQuota);

                Expression<Long> diff = criteriaBuilder.diff(maxQuota, currentQuota);
                Predicate quotaDiffPredicate = criteriaBuilder.lessThanOrEqualTo(diff, value);

                return criteriaBuilder.and(notNullPredicate, limitedQuotaPredicate, quotaDiffPredicate);
            };
        }
    }

}
