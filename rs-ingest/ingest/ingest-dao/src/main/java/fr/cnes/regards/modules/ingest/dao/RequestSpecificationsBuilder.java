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
package fr.cnes.regards.modules.ingest.dao;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;

import static fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications.DISCRIMINANT_ATTRIBUTE;
import static fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications.STATE_ATTRIBUTE;

/**
 * Specification builder to search for {@link AbstractRequest}s with filters as  {@link SearchRequestParameters}
 *
 * @author Stephane Cortine
 * @author Sébastien Binda
 */
public class RequestSpecificationsBuilder
    extends AbstractSpecificationsBuilder<AbstractRequest, SearchRequestParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(after("creationDate", parameters.getCreationDate().getAfter()));
            specifications.add(before("creationDate", parameters.getCreationDate().getBefore()));

            specifications.add(equals("sessionOwner", parameters.getSessionOwner()));

            specifications.add(equals("session", parameters.getSession()));

            specifications.add(useValuesRestriction("id", parameters.getRequestIds()));

            specifications.add(useValuesRestriction("providerId", parameters.getProviderIds()));

            specifications.add(useValuesRestrictionEnumAsString(DISCRIMINANT_ATTRIBUTE, parameters.getRequestTypes()));

            specifications.add(useValuesRestriction(STATE_ATTRIBUTE, parameters.getRequestStates()));

            specifications.add(useValuesRestriction("errorType", parameters.getErrorTypes()));
        }
    }

}
