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
package fr.cnes.regards.modules.acquisition.domain;

import fr.cnes.regards.modules.ingest.domain.sip.ISipState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;

/**
 *
 * This SIP state defines the first steps states in DATA PROVIDER. Following states are inherited from INGEST.
 *
 * <pre>
 *
 *          NOT_SCHEDULED_________NOT_SCHEDULED_INVALID
 *             |
 *          SCHEDULED
 *             |_______ GENERATION_ERROR or SCHEDULED_INTERRUPTED
 *             |
 *         SUBMITTED
 *             |_______ INGESTION_FAILED
 *             |
 *       {@link SIPState}
 *
 * </pre>
 *
 * @author Marc Sordi
 *
 */
public enum ProductSIPState implements ISipState {

    /**
     * SIP is not yet scheduled because related product is not {@link ProductState#FINISHED} or
     * {@link ProductState#COMPLETED}.
     */
    NOT_SCHEDULED,
    /**
     * SIP is not scheduled because related product is {@link ProductState#INVALID}.
     * Data provider has to resubmits its files to fix the problem.
     */
    NOT_SCHEDULED_INVALID,
    /**
     * SIP generation has been scheduled as a job.
     */
    SCHEDULED,
    /**
     * SIP generation interrupted by user
     */
    SCHEDULED_INTERRUPTED,
    /**
     * SIP has not been generated because an error occurs.
     */
    GENERATION_ERROR,
    /**
     * SIP has been generated and submit to INGEST
     */
    SUBMITTED,
    /**
     * SIP has been generated but INGEST refuses to treat it
     */
    INGESTION_FAILED;

    @Override
    public String getName() {
        return this.name();
    }
}
