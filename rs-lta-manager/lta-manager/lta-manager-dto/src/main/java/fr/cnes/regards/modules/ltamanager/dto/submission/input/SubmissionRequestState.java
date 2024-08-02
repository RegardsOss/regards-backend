/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;

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
    VALIDATED(false),
    /**
     * SIP generation on going
     */
    GENERATION_PENDING(false),
    /**
     * SIP successfully generated in OAIS format
     */
    GENERATED(false),
    /**
     * Error during the SIP generation
     */
    GENERATION_ERROR(true),
    /**
     * Product long-term storage on going
     */
    INGESTION_PENDING(false),
    /**
     * Product successfully long-term stored
     */
    DONE(true),
    /**
     * Error during the long-term storage of the OAIS product
     */
    INGESTION_ERROR(true);

    private static final SubmissionRequestState[] ALL_FINISHED_STATE = Arrays.stream(SubmissionRequestState.values())
                                                                             .filter(SubmissionRequestState::isFinalState)
                                                                             .toArray(SubmissionRequestState[]::new);

    private static final SubmissionRequestState[] ALL_NOT_FINISHED_STATE = Arrays.stream(SubmissionRequestState.values())
                                                                                 .filter(state -> !state.isFinalState)
                                                                                 .toArray(SubmissionRequestState[]::new);

    private final boolean isFinalState;

    private SubmissionRequestState(boolean isFinalState) {
        this.isFinalState = isFinalState;
    }

    public boolean isFinalState() {
        return isFinalState;
    }

    public static SubmissionRequestState[] getAllFinishedState() {
        return ALL_FINISHED_STATE;
    }

    public static SubmissionRequestState[] getAllNotFinishedState() {
        return ALL_NOT_FINISHED_STATE;
    }
}
