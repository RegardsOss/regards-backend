/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *              o
 *    __________|_______ REJECTED
 *   |          |
 *   |       CREATED
 *   |          |
 *   |       QUEUED
 *   |          |_______ INVALID
 *   |          |
 *   |        VALID
 * DELETED      |_______ AIP_GEN_ERROR
 *   |          |
 *   |      AIP_CREATED
 *   |__________|________STORE_ERROR
 *   |          |
 *   |        STORED
 *   |          |
 *   |       INDEXED
 *   |__________|
 *   |          |
 *   |_______INCOMPLETE
 *
 * </pre>
 *
 * @author Marc Sordi
 *
 */
public enum SIPState implements ISipState {

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
     * SIP has been validated by the ValidationStep successfully
     */
    VALID,
    /**
     * SIP is invalid (ValidationStep error)
     */
    INVALID,
    /**
     * Error during AIP generation
     */
    AIP_GEN_ERROR,
    /**
     * AIP(s) associated to the SIP has been successfully localy stored and are waiting to be handle by storage
     * microservice.
     */
    AIP_CREATED,
    /**
     * AIP(s) has been successfully stored by storage microservice
     */
    STORED,
    /**
     * One or many AIP(s) failed to be stored by storage microservice
     */
    STORE_ERROR,
    /**
     * SIP has been indexed
     */
    INDEXED,
    /**
     * SIP failed to be indexed
     */
    INDEX_ERROR,
    /**
     * SIP is partially stored.
     */
    INCOMPLETE,
    /**
     * SIP is to be DELETED
     */
    TO_BE_DELETED,
    /**
     * SIP deleted
     */
    DELETED;

    @Override
    public String getName() {
        return this.name();
    }
}
