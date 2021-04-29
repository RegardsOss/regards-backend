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
package fr.cnes.regards.framework.modules.session.management.service.controllers;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.management.dao.ISessionRepository;
import fr.cnes.regards.framework.modules.session.management.dao.SessionSpecifications;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service for session controller
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SessionService {


    @Autowired
    private ISessionRepository sessionRepo;

    @Autowired
    private IPublisher publisher;

    public Page<Session> loadSessions(String name, String state, String source, Pageable pageable) {
        return this.sessionRepo.findAll(SessionSpecifications.search(name, state, source), pageable);
    }

    public void orderDeleteSession(long id) throws EntityNotFoundException {
        Optional<Session> sessionOpt = this.sessionRepo.findById(id);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            this.publisher.publish(new SessionDeleteEvent(session.getSource(), session.getName()));
        } else {
            throw new EntityNotFoundException(id, Session.class);
        }
    }
}
