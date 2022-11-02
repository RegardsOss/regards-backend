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

import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.framework.jpa.utils.CustomPostgresDialect;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPLightParameters;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import java.util.List;

/**
 * @author Stephane Cortine
 */
public class AIPSpecificationsBuilder extends AbstractSpecificationsBuilder<AIPEntityLight, SearchAIPLightParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {

            specifications.add(useValuesRestriction("state", parameters.getAipState()));

            specifications.add(useValuesRestriction("ipType", parameters.getAipIpTypes()));

            specifications.add(after("lastUpdate", parameters.getLastUpdate().getAfter()));
            specifications.add(before("lastUpdate", parameters.getLastUpdate().getBefore()));

            specifications.add(useValuesRestriction("providerId", parameters.getProviderIds()));

            specifications.add(like("sessionOwner", parameters.getSessionOwner()));
            specifications.add(like("session", parameters.getSession()));

            specifications.add(isJsonbArrayContainingOneOfElement("storages", parameters.getStorages()));//jsonb

            specifications.add(isJsonbArrayContainingOneOfElement("categories", parameters.getCategories()));//jsonb

            specifications.add(isJsonbArrayContainingOneOfElement("tags", parameters.getTags()));//jsonb

            specifications.add(equals("last", parameters.getLast()));

            specifications.add(useValuesRestriction("aipIds", parameters.getAipIds()));
        }
    }

    private Specification<AIPEntityLight> isJsonbArrayContainingOneOfElement(String path,
                                                                             ValuesRestriction<String> valuesRestriction) {
        if (valuesRestriction == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Path<Object> attributeRequested = root.get(path);
            Expression<List> allowedValuesConstraint = criteriaBuilder.function(CustomPostgresDialect.EMPTY_STRING_ARRAY,
                                                                                List.class);
            for (String text : valuesRestriction.getValues()) {
                // Append to that array every text researched
                allowedValuesConstraint = criteriaBuilder.function("array_append",
                                                                   List.class,
                                                                   allowedValuesConstraint,
                                                                   criteriaBuilder.function(CustomPostgresDialect.STRING_LITERAL,
                                                                                            String.class,
                                                                                            criteriaBuilder.literal(text)));
            }
            // Check the entity have every text researched
            return criteriaBuilder.isTrue(criteriaBuilder.function(CustomPostgresDialect.JSONB_EXISTS_ANY,
                                                                   Boolean.class,
                                                                   attributeRequested,
                                                                   allowedValuesConstraint));
        };
    }
}
