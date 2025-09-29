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
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;

/**
 * Abstract class for JPA Specification builder to search for AIPs with criteria from {@link SearchAIPsParameters}.
 *
 * @author Sébastien Binda
 **/
public abstract class AbstractAIPSpecificationBuilder<T>
    extends AbstractSpecificationsBuilder<T, SearchAIPsParameters> {

    @Override
    protected void addSpecificationsFromParameters(SearchAIPsParameters parameters) {
        add(useValuesRestriction("state", parameters.getAipStates()));

        add(useValuesRestriction("ipType", parameters.getAipIpTypes()));

        add(after("lastUpdate", parameters.getLastUpdate().getAfter()));
        add(before("lastUpdate", parameters.getLastUpdate().getBefore()));

        add(useValuesRestriction("providerId", parameters.getProviderIds()));

        add(equals("sessionOwner", parameters.getSessionOwner()));
        add(equals("session", parameters.getSession()));

        add(isJsonbArrayContainingOneOfElement("storages", parameters.getStorages()));//jsonb

        add(useValuesRestriction("category", parameters.getCategories()));//jsonb

        add(isJsonbArrayContainingOneOfElement("tags", parameters.getTags()));//jsonb

        add(equals("last", parameters.getLast()));

        add(useValuesRestriction("aipId", parameters.getAipIds()));

        add(equals("originUrn", parameters.getOriginUrn()));

        add(useValuesRestriction("disseminationStatus", parameters.getDisseminationStatus()));

        add(after("creationDate", parameters.getCreationDate().getAfter()));
        add(before("creationDate", parameters.getCreationDate().getBefore()));
    }

}
