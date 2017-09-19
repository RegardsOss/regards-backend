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
package fr.cnes.regards.modules.storage.domain;

import java.time.OffsetDateTime;
import java.util.Collection;

import javax.annotation.Nullable;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.builder.IPBuilder;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 *
 * AIP Builder. Used to create AIP.
 * @author Marc Sordi
 *
 */
public class AIPBuilder extends IPBuilder<AIP> {

    /**
     * Init AIP builder
     * @param type {@link EntityType}
     * @param ipId required information package identifier
     * @param sipId SIP identifier (may be null)
     */
    public AIPBuilder(EntityType type, String ipId, @Nullable String sipId) {
        super(AIP.class, type);
        ip.setIpId(ipId);
        ip.setSipId(sipId);
    }

    /**
     * Add AIP events
     * @param events events to add
     */
    public void addEvents(Event... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        for (Event event : events) {
            Assert.hasLength(event.getComment(), "Event comment is required");
            Assert.notNull(event.getDate(), "Event date is required");
            ip.getHistory().add(event);
        }
    }

    /**
     * Add AIP events
     * @param events events to add
     */
    public void addEvents(Collection<Event> events) {
        Assert.notNull(events, "Collection of events cannot be null");
        addEvents(events.toArray(new Event[events.size()]));
    }

    /**
     * Add an AIP event
     * @param optional type event type key (may be null)
     * @param comment event comment
     * @param date event date
     */
    public void addEvent(@Nullable String type, String comment, OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        addEvents(event);
    }

    /**
     * Add AIP event
     * @param comment event comment
     * @param date event date
     */
    public void addEvent(String comment, OffsetDateTime date) {
        addEvent(null, comment, date);
    }

    /**
     * Add AIP event
     * @param comment event comment
     */
    public void addEvent(String comment) {
        addEvent(null, comment, OffsetDateTime.now());
    }
}
