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
package fr.cnes.regards.modules.ltamanager.dto.submission.input;

/**
 * Available states to indicate the progress of a long-term storage request
 *
 * <pre>
 *     \
 *      \
 *     VALIDATED
 *        |
 *    GENERATION_PENDING
 *        |
 *        |______ GENERATION_ERROR
 *        |
 *     GENERATED
 *        |
 *    INGESTION_PENDING
 *        |
 *        | ______ INGESTION_ERROR
 *        |
 *       DONE
 * </pre>
 *
 * @author Iliana Ghazali
 **/
public enum SubmissionRequestState {

    /**
     * Request is ready to be processed
     */
    VALIDATED,
    /**
     * SIP generation on going
     */
    GENERATION_PENDING,
    /**
     * SIP successfully generated in OAIS format
     */
    GENERATED,
    /**
     * Error during the SIP generation
     */
    GENERATION_ERROR,
    /**
     * Product long-term storage on going
     */
    INGESTION_PENDING,
    /**
     * Product successfully long-term stored
     */
    DONE,
    /**
     * Error during the long-term storage of the OAIS product
     */
    INGESTION_ERROR

}
