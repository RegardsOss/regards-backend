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
package fr.cnes.regards.modules.workermanager.service.sessions;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISessionDeleteService;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ISessionDeleteService} to delete a session irrevocably
 *
 * @author Sébastien Binda
 **/
@Service
@MultitenantTransactional
public class SessionDeleteService implements ISessionDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeleteService.class);

    private RequestService requestService;

    public SessionDeleteService(RequestService requestService) {
        this.requestService = requestService;
    }

    @Override
    public void deleteSession(String source, String session) {
        LOGGER.info("Event received to program the deletion of all requests from session {} of source {}",
                    session,
                    source);
        requestService.scheduleRequestDeletionJob(SearchRequestParameters.build()
                                                                         .withSession(session)
                                                                         .withSource(source));
    }
}