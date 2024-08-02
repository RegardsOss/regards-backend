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
package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISessionDeleteService;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ISessionDeleteService} to delete a session irrevocably
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SessionDeleteService implements ISessionDeleteService {

    @Autowired
    private OAISDeletionService deletionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeleteService.class);

    @Override
    public void deleteSession(String source, String session) {
        LOGGER.info("Event received to program the deletion of all SIP from session {} of source {}", session, source);
        // Run a SessionDeletionJob
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(SessionDeletionMode.IRREVOCABLY);
        dto.withSessionOwner(source).withSession(session);
        deletionService.registerOAISDeletionCreator(dto);
    }
}