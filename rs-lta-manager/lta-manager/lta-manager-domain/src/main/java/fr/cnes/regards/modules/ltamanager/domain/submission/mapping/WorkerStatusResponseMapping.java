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
package fr.cnes.regards.modules.ltamanager.domain.submission.mapping;

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.workermanager.dto.events.out.ResponseStatus;

/**
 * Map between worker {@link ResponseStatus} received and {@link SubmissionRequestState}.
 *
 * @author Iliana Ghazali
 **/
public enum WorkerStatusResponseMapping {

    SKIPPED_MAP(ResponseStatus.SKIPPED, SubmissionRequestState.GENERATION_ERROR),

    GRANTED_MAP(ResponseStatus.GRANTED, SubmissionRequestState.GENERATION_PENDING),

    INVALID_CONTENT_MAP(ResponseStatus.INVALID_CONTENT, SubmissionRequestState.GENERATION_ERROR),

    ERROR_MAP(ResponseStatus.ERROR, SubmissionRequestState.GENERATION_ERROR),

    SUCCESS_MAP(ResponseStatus.SUCCESS, SubmissionRequestState.GENERATED);

    private final ResponseStatus originStatus;

    private final SubmissionRequestState mappedState;

    WorkerStatusResponseMapping(ResponseStatus originStatus, SubmissionRequestState mappedState) {
        this.originStatus = originStatus;
        this.mappedState = mappedState;
    }

    public ResponseStatus getOriginStatus() {
        return originStatus;
    }

    public SubmissionRequestState getMappedState() {
        return mappedState;
    }

    public static WorkerStatusResponseMapping findMappedStatus(ResponseStatus requestedOriginStatus) {
        WorkerStatusResponseMapping[] mappedEnums = WorkerStatusResponseMapping.values();
        for (WorkerStatusResponseMapping mappedProperty : mappedEnums) {
            if (mappedProperty.originStatus == requestedOriginStatus) {
                return mappedProperty;
            }
        }
        return null;
    }

}
