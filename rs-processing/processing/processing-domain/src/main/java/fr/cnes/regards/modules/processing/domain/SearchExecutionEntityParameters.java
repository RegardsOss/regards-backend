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
package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * @author Stephane Cortine
 */
public class SearchExecutionEntityParameters {

    @Schema(description = "Filter on id of business process")
    private String processBusinessId;

    @Schema(description = "Filter on email of user")
    private ValuesRestriction<String> userEmail;

    @Schema(description = "Filter on creation date")
    private DatesRangeRestriction creationDate = new DatesRangeRestriction();

    @Schema(description = "Filter on status of execution")
    private ValuesRestriction<ExecutionStatus> status;

    public String getProcessBusinessId() {
        return processBusinessId;
    }

    public void setProcessBusinessId(String processBusinessId) {
        this.processBusinessId = processBusinessId;
    }

    public SearchExecutionEntityParameters withProcessBusinessId(String processBusinessId) {
        this.processBusinessId = processBusinessId;
        return this;
    }

    public ValuesRestriction<String> getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(ValuesRestriction<String> userEmail) {
        this.userEmail = userEmail;
    }

    public SearchExecutionEntityParameters withUserEmailIncluded(Collection<String> userEmails) {
        this.userEmail = new ValuesRestriction<String>().withInclude(userEmails);
        return this;
    }

    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DatesRangeRestriction creationDate) {
        this.creationDate = creationDate;
    }

    public SearchExecutionEntityParameters withCreationDateBefore(OffsetDateTime before) {
        this.creationDate.setBefore(before);
        return this;
    }

    public SearchExecutionEntityParameters withCreationDateAfter(OffsetDateTime after) {
        this.creationDate.setAfter(after);
        return this;
    }

    public ValuesRestriction<ExecutionStatus> getStatus() {
        return status;
    }

    public void setStatus(ValuesRestriction<ExecutionStatus> status) {
        this.status = status;
    }

    public SearchExecutionEntityParameters withStatusIncluded(Collection<ExecutionStatus> status) {
        this.status = new ValuesRestriction<ExecutionStatus>().withInclude(status);
        return this;
    }

    public SearchExecutionEntityParameters withStatusExcluded(Collection<ExecutionStatus> status) {
        this.status = new ValuesRestriction<ExecutionStatus>().withExclude(status);
        return this;
    }
}
