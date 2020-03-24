/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.test.transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * A service that publish an event in a transaction to test tenant binding.
 * @author Marc Sordi
 */
@Service
public class PublishService {

    private final IPublisher publisher;

    public PublishService(IPublisher publisher) {
        this.publisher = publisher;
    }

    @Transactional
    public <T extends IPollable> void transactionalPublish(T event, boolean crash, boolean purgeQueue) {
        publisher.publish(event, purgeQueue);
        // Do something : for instance, store in database
        if (crash) {
            // An error occurs : transaction manager will rollback database and restore AMQP event on server
            throw new UnsupportedOperationException("Publish fails!");
        }
    }
}
