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
package fr.cnes.regards.modules.acquisition.domain;

/**
 *
 * SIP lifecycle
 *
 * <pre>
*
*          NOT_SCHEDULED
*             |
*          SCHEDULED
*             |_______ GENERATION_ERROR
*             |
*          GENERATED
*             |_______ INGESTION_ERROR
*             |
*          INGESTED
 *
 * </pre>
 *
 * @author Marc Sordi
 *
 */
public enum ProductSIPState {

    /**
     * SIP is not yet scheduled because related product is not {@link ProductStatus#FINISHED} or
     * {@link ProductStatus#COMPLETED}.
     */
    NOT_SCHEDULED,
    /**
     * SIP generation has been scheduled as a job.
     */
    SCHEDULED,
    /**
     * SIP has been generated.
     */
    GENERATED,
    /**
     * SIP has not been generated because an error occurs.
     */
    GENERATION_ERROR,
    /**
     * SIP has been sent to INGEST successfully
     */
    INGESTED,
    /**
     * SIP has been sent to INGEST but REJECTED for some reasons
     */
    INGESTION_ERROR;
}
