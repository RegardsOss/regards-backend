/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.entity;

/**
 *
 * SIP lifecycle
 *
 * <pre>
 *           o
 *           |________REJECTED
 *           |
 *        CREATED_______
 *           |          | retry
 *        QUEUED        |
 *           |________ERROR (invalid, generation error, ...)
 *           |          |
 *       INGESTED       |
 *           |          | delete
 *       TO_BE_DELETED  |
 *           |          |
 *        DELETED_______|
 * </pre>
 *
 * @author Marc Sordi
 *
 */
public enum SIPState {

    /**
     * SIP is stored in database and has to be processed
     */
    CREATED,
    /**
     * Invalid SIP or error during its storage
     */
    REJECTED,
    /**
     * SIP is queued to be processed.
     */
    QUEUED,
    /**
     * SIP processing fails
     */
    ERROR,
    /**
     * SIP processing ends successfully
     */
    INGESTED,
    /**
     * SIP is to be DELETED
     */
    TO_BE_DELETED,
    /**
     * SIP deleted
     */
    DELETED;
}
