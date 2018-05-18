/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * A service that poll an event in a transaction enabling acknowledgement feature.
 *
 * @author Marc Sordi
 *
 */
@Service
public class PollableService {

    private final IPoller poller;

    public PollableService(IPoller poller) {
        this.poller = poller;
    }

    @Transactional
    public <T extends IPollable> TenantWrapper<T> transactionalPoll(Class<T> eventType, boolean crash) {
        TenantWrapper<T> wrapper = poller.poll(eventType);
        // Do something : for instance, store in database not to lose event
        if (crash) {
            // An error occurs : transaction manager will rollback database and restore AMQP event on server
            throw new UnsupportedOperationException("Poll fails!");
        } else {
            return wrapper;
        }
    }
}
