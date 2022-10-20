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
package fr.cnes.regards.modules.ltamanager.service.session;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionInfoDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to manage sessions infos
 *
 * @author Thomas GUILLOU
 **/
@Service
@MultitenantTransactional
public class SubmissionSessionService {

    private final IAuthenticationResolver authenticationResolver;

    private final ISubmissionRequestRepository submissionRequestRepository;

    public SubmissionSessionService(IAuthenticationResolver authenticationResolver,
                                    ISubmissionRequestRepository submissionRequestRepository) {
        this.authenticationResolver = authenticationResolver;
        this.submissionRequestRepository = submissionRequestRepository;
    }

    /**
     * Get the global session info of a specific session.
     *
     * @throws EntityNotFoundException if session not exists
     */
    public SessionInfoDTO getGlobalSessionInfo(String session) throws EntityNotFoundException {
        String owner = authenticationResolver.getUser();
        List<String> states = submissionRequestRepository.findStatesBySessionAndOwner(session, owner);
        if (states.isEmpty()) {
            throw new EntityNotFoundException(session, SubmissionRequest.class);
        }
        return SessionInfoCreator.convert(states);
    }
}
