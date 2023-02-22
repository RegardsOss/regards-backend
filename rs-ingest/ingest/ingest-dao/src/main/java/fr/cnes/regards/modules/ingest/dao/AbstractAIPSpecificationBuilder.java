/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

/**
 * Abstract class for JPA Specification builder to search for AIPs with criteria from {@link SearchAIPsParameters}.
 *
 * @author Sébastien Binda
 **/
public abstract class AbstractAIPSpecificationBuilder<T>
    extends AbstractSpecificationsBuilder<T, SearchAIPsParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {

            specifications.add(useValuesRestriction("state", parameters.getAipStates()));

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

            specifications.add(useValuesRestriction("aipId", parameters.getAipIds()));

            specifications.add(equals("originUrn", parameters.getOriginUrn()));
        }
    }

}
