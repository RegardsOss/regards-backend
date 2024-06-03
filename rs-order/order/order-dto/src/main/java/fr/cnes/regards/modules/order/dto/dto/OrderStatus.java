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
package fr.cnes.regards.modules.order.dto.dto;

import java.util.Arrays;

/**
 * Order status
 *
 * @author oroussel
 */
public enum OrderStatus {
    /**
     * Order is in creation, no associated jobs exist or are planned to be executed
     */
    PENDING,
    /**
     * At least one associated job is planned to be executed
     */
    RUNNING,
    /**
     * Associated jobs are asked to be stopped (or already stopped). Order can be resumed later
     */
    PAUSED,
    /**
     * Order has been detected as expired and soon will be paused (very intermediate status to avoid 2 transactions
     * retrieving same order as 'expired' and try manage it)
     */
    EXPIRED,
    /**
     * No data file is available, completion percentage is at 100%
     */
    FAILED,
    /**
     * One or more data files have failed but at least one is available, completion percentage is at 100%
     */
    DONE_WITH_WARNING,
    /**
     * All data files are available, completion percentage is at 100%
     */
    DONE,
    /**
     * User asked for order deletion, everything is still available (dataset tasks, files tasks, jobs) except order
     * data files that are removed.
     * Only a {@link OrderStatus#PAUSED}, {@link OrderStatus#DONE}, {@link OrderStatus#DONE_WITH_WARNING},
     * {@link OrderStatus#FAILED} order can be {@link OrderStatus#DELETED} (with no running jobs)
     */
    DELETED,

    /**
     * Order is in error, associated jobs are in error, problem during the deleting of order.
     */
    ERROR;

    public boolean isOneOfStatuses(OrderStatus... status) {
        return Arrays.stream(status).anyMatch(this::equals);
    }
}
