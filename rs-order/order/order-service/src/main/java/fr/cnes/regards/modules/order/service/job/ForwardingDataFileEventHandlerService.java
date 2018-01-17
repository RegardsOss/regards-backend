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
package fr.cnes.regards.modules.order.service.job;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;

/**
 * DataFileEvent handler that forward received DataFileEvents to subscribed handlers.
 * That permits to override AMQP queue handler name limitation (one handler by class name) so that every StorageFilesJob
 * can listen to DataFileEvents
 * @author oroussel
 */
@Service
public class ForwardingDataFileEventHandlerService implements IForwardingDataFileEventHandlerService {
    @Autowired
    private ISubscriber subscriber;

    private final Set<IHandler<DataFileEvent>> handlers = new HashSet<>();

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        subscriber.subscribeTo(DataFileEvent.class, this);
    }

    /**
     * Subscribe to receive events
     */
    public void subscribe(IHandler<DataFileEvent> receiver) {
        handlers.add(receiver);
    }

    /**
     * Unsubscribe to avoid keeping all handlers in memory
     */
    public void unsubscribe(IHandler<DataFileEvent> receiver) {
        handlers.remove(receiver);
    }

    /**
     * Receive an event and forward it to all subscribed handlers
     */
    @Override
    public void handle(TenantWrapper<DataFileEvent> wrapper) {
        handlers.forEach(h -> h.handle(wrapper.clone()));
    }
}
