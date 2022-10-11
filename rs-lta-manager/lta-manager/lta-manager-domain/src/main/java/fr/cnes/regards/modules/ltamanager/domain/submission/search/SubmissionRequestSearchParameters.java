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
package fr.cnes.regards.modules.ltamanager.domain.submission.search;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import io.swagger.v3.oas.annotations.media.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.validation.Valid;
import java.util.Objects;

/**
 * @author Iliana Ghazali
 **/
public class SubmissionRequestSearchParameters implements AbstractSearchParameters<SubmissionRequest> {

    @Schema(description = "Request sender.", nullable = true)
    @Nullable
    private final String owner;

    @Schema(description = "Submission request session", nullable = true)
    @Nullable
    private final String session;

    @Schema(description = "Product datatype.", nullable = true)
    @Nullable
    private final String datatype;

    @Schema(description = "Submission request creation date.", nullable = true)
    @Nullable
    @Valid
    private final DatesRangeRestriction creationDate;

    @Schema(description = "Submission request last update date.", nullable = true)
    @Nullable
    @Valid
    private final DatesRangeRestriction statusDate;

    @Schema(description = "Included submission request states (only INCLUDED MODE is accepted).", nullable = true)
    @Nullable
    @Valid
    private final ValuesRestriction<SubmissionRequestState> statusesRestriction;

    @Schema(description = "Restricted ids (INCLUDED or EXCLUDED).", nullable = true)
    @Nullable
    @Valid
    private final ValuesRestriction<String> idsRestriction;

    public SubmissionRequestSearchParameters(@Nullable String owner,
                                             @Nullable String session,
                                             @Nullable String datatype,
                                             @Nullable DatesRangeRestriction creationDate,
                                             @Nullable DatesRangeRestriction statusDate,
                                             @Nullable ValuesRestriction<SubmissionRequestState> statusesRestriction,
                                             @Nullable ValuesRestriction<String> idsRestriction) {
        this.owner = owner;
        this.session = session;
        this.datatype = datatype;
        this.creationDate = creationDate;
        this.statusDate = statusDate;
        this.statusesRestriction = statusesRestriction;
        this.idsRestriction = idsRestriction;
    }

    @Nullable
    public String getOwner() {
        return owner;
    }

    @Nullable
    public String getSession() {
        return session;
    }

    @Nullable
    public String getDatatype() {
        return datatype;
    }

    @Nullable
    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    @Nullable
    public DatesRangeRestriction getStatusDate() {
        return statusDate;
    }

    @Nullable
    public ValuesRestriction<SubmissionRequestState> getStatusesRestriction() {
        return statusesRestriction;
    }

    @Nullable
    public ValuesRestriction<String> getIdsRestriction() {
        return idsRestriction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmissionRequestSearchParameters that = (SubmissionRequestSearchParameters) o;
        return Objects.equals(owner, that.owner)
               && Objects.equals(session, that.session)
               && Objects.equals(datatype,
                                 that.datatype)
               && Objects.equals(creationDate, that.creationDate)
               && Objects.equals(statusDate, that.statusDate)
               && Objects.equals(statusesRestriction, that.statusesRestriction)
               && Objects.equals(idsRestriction, that.idsRestriction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, session, datatype, creationDate, statusDate, statusesRestriction, idsRestriction);
    }

    @Override
    public String toString() {
        return "SubmissionRequestSearchCriterion{"
               + "owner='"
               + owner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", datatype='"
               + datatype
               + '\''
               + ", creationDate="
               + creationDate
               + ", statusDate="
               + statusDate
               + ", statusesRestriction="
               + statusesRestriction
               + ", idsRestriction="
               + idsRestriction
               + '}';
    }
}
