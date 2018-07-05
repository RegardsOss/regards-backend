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
package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import java.time.OffsetDateTime;

/**
 * Tool class to generate {@link AIPSession} entities
 * @author Léo Mieulet
 */
public final class AIPSessionBuilder {

    private AIPSessionBuilder() {
    }

    /**
     * Build a {@link AIPSession} entity for the given session id
     * @param sessionId {@link String}
     * @return {@link AIPSession}
     */
    public static AIPSession build(String sessionId) {
        AIPSession session = new AIPSession();
        session.setId(sessionId);
        session.setLastActivationDate(OffsetDateTime.now());
        return session;
    }

}
