/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

public class MockEventHandler implements IHandler<AIPEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MockEventHandler.class);

    private final Set<AIPEvent> receivedEvents = Sets.newHashSet();

    @Override
    public void handle(TenantWrapper<AIPEvent> wrapper) {
        LOG.info("[MOCK EVENT HANDLER] New AIPEvent Recieved- {} - {}", wrapper.getContent().getAipState().toString(),
                 wrapper.getContent().getAipId());
        receivedEvents.add(wrapper.getContent());
    }

    public Set<AIPEvent> getReceivedEvents() {
        return receivedEvents;
    }

    public void clear() {
        receivedEvents.clear();
    }

    public void log() {
        receivedEvents.forEach(event -> LOG.info("Received event : ipId:{}, state:{}", event.getAipId(),
                                                 event.getAipState()));
    }

}
