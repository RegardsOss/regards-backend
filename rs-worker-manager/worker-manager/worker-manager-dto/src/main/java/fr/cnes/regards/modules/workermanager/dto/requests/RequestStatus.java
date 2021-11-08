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
package fr.cnes.regards.modules.workermanager.dto.requests;

/**
 * Enumeration to define all status for a {@link RequestDTO}
 *
 * @author Sébastien Binda
 */
public enum RequestStatus {

    /**
     * Request has been successfully dispatched to a worker and is waiting for response.
     */
    DISPATCHED,

    /**
     * Request does not match any registered worker.
     * Request will be dispatch as soon as a worker handling the content type of the request register.
     */
    NO_WORKER_AVAILABLE,

    /**
     * Request has been handled by a worker, and it sent back the granted status.
     */
    RUNNING,

    /**
     * Request has been handled by a worker, and it sent back the rejected status.
     */
    INVALID_CONTENT,

    /**
     * Request has been handled by a worker, and it sent back the successfully status.
     */
    SUCCESS,

    /**
     * Request has been handled by a worker, and it sent back that some error has been raise during request handling
     */
    ERROR,

    /**
     * Request will be re-dispatch soon
     */
    TO_DISPATCH,

}
