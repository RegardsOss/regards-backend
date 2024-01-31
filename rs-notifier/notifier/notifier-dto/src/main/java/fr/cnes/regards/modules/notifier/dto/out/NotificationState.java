/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dto.out;

/**
 * State diagram is:
 * <pre>
 *                  |
 *          DENIED _|_ GRANTED
 *                        |
 *                 ERROR _|_ TO_SCHEDULE_BY_RECIPIENT <---------|
 *                    |           |                             |
 *                    |       SCHEDULED                         |
 *                    |           |                             |
 *                    |    ERROR _|_ SUCCESS                    |
 *                    |______|__________________________________|
 * </pre>
 *
 * @author Sylvain Vissiere-Guerinet
 */
public enum NotificationState {
    /**
     * Denied and not registered.
     */
    DENIED(false),
    /**
     * Accepted and registered. Rule matching has not yet been done.
     */
    GRANTED(true),
    /**
     * Rule matching has been done. Jobs for each recipient should be scheduled(and created)
     */
    TO_SCHEDULE_BY_RECIPIENT(false),
    /**
     * Jobs for each recipient has been scheduled.
     */
    SCHEDULED(true),
    /**
     * All recipients have been successfully handled.
     */
    SUCCESS(false),
    /**
     * At least one recipient have not been successfully handled.
     */
    ERROR(false);

    private final boolean running;

    /**
     * Indicates if the request state is a running state or not.
     * Not running state means the request is in a stable state and no jobs are associated to it.
     */
    NotificationState(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }
}
