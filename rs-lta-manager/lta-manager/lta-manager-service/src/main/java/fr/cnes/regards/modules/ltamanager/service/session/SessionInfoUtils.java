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
package fr.cnes.regards.modules.ltamanager.service.session;

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionStatus;

import java.util.Arrays;
import java.util.List;

/**
 * Methods utils that manipulate SessionStatus
 *
 * @author Thomas GUILLOU
 **/
public final class SessionInfoUtils {

    private SessionInfoUtils() {
    }

    public static SessionStatus getSessionStatusFromStrings(List<String> statesAsString) {
        return getSessionStatus(statesAsString.stream().map(SubmissionRequestState::valueOf).toList());
    }

    /**
     * Generate global session status from requests status.
     */
    public static SessionStatus getSessionStatus(List<SubmissionRequestState> states) {
        SessionStatus sessionStatus;
        boolean anyRequestError = anyRequestHaveStatus(states,
                                                       SubmissionRequestState.INGESTION_ERROR,
                                                       SubmissionRequestState.GENERATION_ERROR);
        if (anyRequestError) {
            sessionStatus = SessionStatus.ERROR;
        } else {
            boolean anyRequestPending = anyRequestHaveStatus(states,
                                                             SubmissionRequestState.INGESTION_PENDING,
                                                             SubmissionRequestState.GENERATION_PENDING,
                                                             SubmissionRequestState.VALIDATED,
                                                             SubmissionRequestState.GENERATED);
            // if not any pending or error, then session is done
            sessionStatus = anyRequestPending ? SessionStatus.RUNNING : SessionStatus.DONE;
        }
        return sessionStatus;
    }

    private static boolean anyRequestHaveStatus(List<SubmissionRequestState> requestStates,
                                                SubmissionRequestState... statesToCheck) {
        return Arrays.stream(statesToCheck).anyMatch(requestStates::contains);
    }
}
