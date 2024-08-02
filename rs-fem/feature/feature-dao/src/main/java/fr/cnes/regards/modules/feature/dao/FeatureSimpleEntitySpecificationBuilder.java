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
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.feature.domain.FeatureSimpleEntity;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;

/**
 * @author Stephane Cortine
 */
public class FeatureSimpleEntitySpecificationBuilder
    extends AbstractSpecificationsBuilder<FeatureSimpleEntity, SearchFeatureSimpleEntityParameters> {

    private static final String DISSEMINATION_PENDING_FILED = "disseminationPending";

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(useValuesRestriction("id", parameters.getFeatureIds()));
            specifications.add(useValuesRestriction("providerId", parameters.getProviderIds()));

            specifications.add(equals("model", parameters.getModel()));

            specifications.add(equals("sessionOwner", parameters.getSource()));
            specifications.add(equals("session", parameters.getSession()));

            specifications.add(after("lastUpdate", parameters.getLastUpdate().getAfter()));
            specifications.add(before("lastUpdate", parameters.getLastUpdate().getBefore()));

            if (parameters.getDisseminationStatus() != null) {
                switch (parameters.getDisseminationStatus()) {
                    case NONE -> {
                        specifications.add(isNull(DISSEMINATION_PENDING_FILED));
                    }
                    case PENDING -> {
                        specifications.add(equals(DISSEMINATION_PENDING_FILED, true));
                    }
                    case DONE -> {
                        specifications.add(equals(DISSEMINATION_PENDING_FILED, false));
                    }
                    default -> {
                        // nothing to do
                    }
                }
            }

        }
    }
}
