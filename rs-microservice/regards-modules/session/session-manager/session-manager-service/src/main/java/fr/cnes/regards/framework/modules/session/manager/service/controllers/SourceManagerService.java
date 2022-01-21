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
package fr.cnes.regards.framework.modules.session.manager.service.controllers;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.manager.dao.ISourceManagerRepository;
import fr.cnes.regards.framework.modules.session.manager.dao.SourceManagerSpecifications;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for source controller
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class SourceManagerService {

    @Autowired
    private ISourceManagerRepository sourceRepo;

    @Autowired
    private IPublisher publisher;

    public Page<Source> loadSources(String name, String state, Pageable pageable) {
        return this.sourceRepo.findAll(SourceManagerSpecifications.search(name, state), pageable);
    }

    public void orderDeleteSource(String name) throws EntityNotFoundException {
        Optional<Source> sourceOpt = this.sourceRepo.findByName(name);
        if (sourceOpt.isPresent()) {
            publisher.publish(new SourceDeleteEvent(name));
        } else {
            throw new EntityNotFoundException(name, Source.class);
        }
    }

    public Set<String> retrieveSourcesNames(String name) {
        return this.sourceRepo.findAllSourcesNames(name);
    }
}