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
package fr.cnes.regards.modules.workermanager.dto.requests;

import java.time.OffsetDateTime;

/**
 * DTO POJO for WorkerManagerRequest
 *
 * @author Sébastien Binda
 */
public class RequestDTO {

    private String requestId;

    private OffsetDateTime creationDate;

    private String contentType;

    private String source;

    private String session;

    private RequestStatus status;

    private String dispatchedWorkerType;

    private String error;

    public RequestDTO(String requestId,
                      OffsetDateTime creationDate,
                      String contentType,
                      String source,
                      String session,
                      RequestStatus status,
                      String dispatchedWorkerType,
                      String error) {
        this.requestId = requestId;
        this.creationDate = creationDate;
        this.contentType = contentType;
        this.source = source;
        this.session = session;
        this.status = status;
        this.dispatchedWorkerType = dispatchedWorkerType;
        this.error = error;
    }

    public String getRequestId() {
        return requestId;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSource() {
        return source;
    }

    public String getSession() {
        return session;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getDispatchedWorkerType() {
        return dispatchedWorkerType;
    }

    public String getError() {
        return error;
    }
}
