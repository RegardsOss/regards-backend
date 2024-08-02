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
package fr.cnes.regards.modules.ltamanager.domain.submission.mapping;

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;

/**
 * Map between ingest RequestInfo status received and {@link SubmissionRequestState}.
 *
 * @author Iliana Ghazali
 **/
public enum IngestStatusResponseMapping {

    GRANTED_MAP("GRANTED", SubmissionRequestState.INGESTION_PENDING),

    DENIED_MAP("DENIED", SubmissionRequestState.INGESTION_ERROR),

    ERROR_MAP("ERROR", SubmissionRequestState.INGESTION_ERROR),

    DELETED_MAP("DELETED", SubmissionRequestState.INGESTION_ERROR),

    SUCCESS_MAP("SUCCESS", SubmissionRequestState.DONE);

    private final String originStatus;

    private final SubmissionRequestState mappedState;

    IngestStatusResponseMapping(String originStatus, SubmissionRequestState mappedState) {
        this.originStatus = originStatus;
        this.mappedState = mappedState;
    }

    public String getOriginStatus() {
        return originStatus;
    }

    public SubmissionRequestState getMappedState() {
        return mappedState;
    }
}
