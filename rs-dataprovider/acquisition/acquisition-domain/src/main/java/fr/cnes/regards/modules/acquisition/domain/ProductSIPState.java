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

import fr.cnes.regards.modules.ingest.domain.entity.ISipState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 *
 * This SIP state defines the first steps states in DATA PROVIDER. Following states are inherited from INGEST.
 *
 * <pre>
 *
 *          NOT_SCHEDULED
 *             |
 *          SCHEDULED
 *             |_______ GENERATION_ERROR
 *             |
 *          GENERATED
 *             |
 *      SUBMISSION_SCHEDULED
 *             |____________ SUBMISSION_ERROR
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
     * SIP submission has been scheduled
     */
    SUBMISSION_SCHEDULED,
    /**
     * An error occurs during submission job
     */
    SUBMISSION_ERROR;

    @Override
    public String getName() {
        return this.name();
    }
}
