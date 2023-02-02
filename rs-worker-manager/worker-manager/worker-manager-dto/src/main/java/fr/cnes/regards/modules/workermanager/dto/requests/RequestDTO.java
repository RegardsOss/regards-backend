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
import java.util.Objects;

/**
 * DTO POJO for WorkerManagerRequest
 *
 * @author Sébastien Binda
 */
public class RequestDTO {

    private final String requestId;

    private final OffsetDateTime creationDate;

    private final String contentType;

    private final Integer step;

    private final String source;

    private final String session;

    private final RequestStatus status;

    private final String dispatchedWorkerType;

    private final String error;

    public RequestDTO(String requestId,
                      OffsetDateTime creationDate,
                      String contentType,
                      Integer step,
                      String source,
                      String session,
                      RequestStatus status,
                      String dispatchedWorkerType,
                      String error) {
        this.requestId = requestId;
        this.creationDate = creationDate;
        this.contentType = contentType;
        this.step = step;
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

    public int getStep() {
        return step;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestDTO that = (RequestDTO) o;
        return requestId.equals(that.requestId)
               && creationDate.equals(that.creationDate)
               && contentType.equals(that.contentType)
               && source.equals(that.source)
               && session.equals(that.session)
               && status == that.status
               && Objects.equals(dispatchedWorkerType, that.dispatchedWorkerType)
               && Objects.equals(error, that.error)
               && Objects.equals(step, that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId,
                            creationDate,
                            contentType,
                            step,
                            source,
                            session,
                            status,
                            dispatchedWorkerType,
                            error);
    }

    @Override
    public String toString() {
        return "RequestDTO{"
               + "requestId='"
               + requestId
               + '\''
               + ", creationDate="
               + creationDate
               + ", contentType='"
               + contentType
               + '\''
               + ", step="
               + step
               + ", source='"
               + source
               + '\''
               + ", session='"
               + session
               + '\''
               + ", status="
               + status
               + ", dispatchedWorkerType='"
               + dispatchedWorkerType
               + '\''
               + ", error='"
               + error
               + '\''
               + '}';
    }

}
