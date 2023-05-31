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
package fr.cnes.regards.modules.ltamanager.domain.submission;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Linked to {@link SubmissionRequest}
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class SubmissionStatus {

    public static final String CREATION_DATE_FIELD_NAME = "creationDate";

    public static final String STATUS_DATE_FIELD_NAME = "statusDate";

    public static final String EXPIRY_DATE_FIELD_NAME = "expiryDate";

    public static final String STATUS_FIELD_NAME = "status";

    public static final List<String> STATUS_FIELD_NAMES = List.of(STATUS_FIELD_NAME,
                                                            EXPIRY_DATE_FIELD_NAME,
                                                            STATUS_DATE_FIELD_NAME,
                                                            CREATION_DATE_FIELD_NAME);

    @Column(name = "creation_date", nullable = false, updatable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "creationDate is required")
    private OffsetDateTime creationDate;

    @Column(name = "expiry_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "expiryDate is required")
    private OffsetDateTime expiryDate;

    @Column(name = "status_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "statusDate is required")
    private OffsetDateTime statusDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "status is required")
    private SubmissionRequestState status;

    @Column
    @Type(type = "text")
    @Nullable
    private String message;

    public SubmissionStatus() {
        // no-args constructor for jpa
    }

    public SubmissionStatus(OffsetDateTime creationDate,
                            OffsetDateTime statusDate,
                            Integer requestExpiresInHour,
                            SubmissionRequestState status,
                            @Nullable String message) {
        Assert.notNull(creationDate, "creationDate is mandatory !");
        Assert.notNull(statusDate, "statusDate is mandatory !");
        Assert.notNull(status, "status is mandatory !");
        Assert.notNull(requestExpiresInHour,
                       "requestExpiresInHour is mandatory ! Make sure other constraints are satisfied");

        this.creationDate = creationDate;
        this.statusDate = statusDate;
        this.expiryDate = creationDate.plusHours(requestExpiresInHour);
        this.status = status;
        this.message = message;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public OffsetDateTime getExpiryDate() {
        return expiryDate;
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public SubmissionRequestState getStatus() {
        return status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmissionStatus that = (SubmissionStatus) o;
        return creationDate.equals(that.creationDate)
               && statusDate.equals(that.statusDate)
               && status == that.status
               && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, statusDate, status, message);
    }

    @Override
    public String toString() {
        return "SubmissionStatus{"
               + "creationDate="
               + creationDate
               + ", expiryDate="
               + expiryDate
               + ", statusDate="
               + statusDate
               + ", status="
               + status
               + ", message='"
               + message
               + '\''
               + '}';
    }
}
