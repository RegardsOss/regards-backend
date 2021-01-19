/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.sessionmanager.client;

import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

public interface ISessionNotificationClient {

    /**
     * Allows to mutate step affected by {@link #increment(String, String, String, SessionNotificationState, long)},
     * {@link #decrement(String, String, String, SessionNotificationState, long)}
     * or {@link #stepValue(String, String, String, SessionNotificationState, String)} actions.
     * <br/> Default : spring.application.name
     */
    void setStep(String step);

    void increment(String sessionOwner, String session, String property, SessionNotificationState notifState,
            long value);

    void decrement(String sessionOwner, String session, String property, SessionNotificationState notifState,
            long value);

    void stepValue(String sessionOwner, String session, String property, SessionNotificationState notifState,
            String value);
}
