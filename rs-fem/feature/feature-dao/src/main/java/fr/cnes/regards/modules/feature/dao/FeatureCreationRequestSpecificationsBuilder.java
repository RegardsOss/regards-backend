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
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureCreationRequestParameters;

/**
 * @author Stephane Cortine
 */
public class FeatureCreationRequestSpecificationsBuilder extends
    AbstractFeatureRequestSpecificationsBuilder<FeatureCreationRequest, SearchFeatureCreationRequestParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        specifications.add(equals("metadata.sessionOwner", parameters.getSource()));

        specifications.add(equals("metadata.session", parameters.getSession()));

        specifications.add(useValuesRestrictionLike("providerId", parameters.getProviderIds()));

        specifications.add(useValuesRestriction(AbstractRequest.COLUMN_STATE, parameters.getStates()));

        specifications.add(after("registrationDate", parameters.getLastUpdate().getAfter()));
        specifications.add(before("registrationDate", parameters.getLastUpdate().getBefore()));
    }

}