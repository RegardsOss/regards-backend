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
package fr.cnes.regards.modules.ingest.domain.builder;

import java.time.OffsetDateTime;

import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;

/**
 * Tool class to generate {@link SIPSession} entities
 * @author Sébastien Binda
 */
public final class SIPSessionBuilder {

    private SIPSessionBuilder() {
    }

    /**
     * Build a {@link SIPSession} entity for the given session id
     * @param sessionId {@link String}
     * @return {@link SIPSession}
     */
    public static SIPSession build(String sessionId) {
        SIPSession session = new SIPSession();
        session.setId(sessionId);
        session.setLastActivationDate(OffsetDateTime.now());
        return session;
    }

}
