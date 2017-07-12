/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.test.event.transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * A service that publish an event in a transaction to test tenant binding.
 *
 * @author Marc Sordi
 *
 */
@Service
public class PublishService {

    /**
     * Poller
     */
    private final IPublisher publisher;

    public PublishService(IPublisher pPublisher) {
        this.publisher = pPublisher;
    }

    @Transactional
    public <T extends IPollable> void transactionalPublish(T pEvent, boolean pCrash) {
        publisher.publish(pEvent);
        // Do something : for instance, store in database
        if (pCrash) {
            // An error occurs : transaction manager will rollback database and restore AMQP event on server
            throw new UnsupportedOperationException("Publish fails!");
        }
    }
}
