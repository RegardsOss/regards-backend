/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.dao;

import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.domain.request.Request;

/**
 * @author Théo Lasserre
 */
public class RequestSpecificationsBuilder extends AbstractSpecificationsBuilder<Request, SearchRequestParameters> {

    @Override
    protected void addSpecificationsFromParameters() {
        if (parameters != null) {
            specifications.add(useDatesRestriction("creationDate", parameters.getCreationDate()));
            specifications.add(like("source", parameters.getSource()));
            specifications.add(like("session", parameters.getSession()));
            specifications.add(like("dispatchedWorkerType", parameters.getDispatchedWorkerType()));
            specifications.add(useValuesRestriction("status", parameters.getStatuses()));
            specifications.add(useValuesRestriction("id", parameters.getIds()));
            specifications.add(useValuesRestriction("contentType", parameters.getContentTypes()));
        }
    }
}
