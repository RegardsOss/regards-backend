/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public abstract class AbstractWorkloadEngine implements IWorkloadEngine {

    private final IWorkloadEngineRepository engineRepo;

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    @Autowired
    public AbstractWorkloadEngine(
            IWorkloadEngineRepository engineRepo,
            ISubscriber subscriber,
            IPublisher publisher
    ) {
        this.engineRepo = engineRepo;
        this.subscriber = subscriber;
        this.publisher = publisher;
    }

    @PostConstruct
    public final void register() {
        engineRepo.register(this);
    }

}
