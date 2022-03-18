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
package fr.cnes.regards.modules.workermanager.domain.request;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;

/**
 * Allows complex research on {@link Request}
 *
 * @author Théo Lasserre
 */
public class SearchRequestParameters implements AbstractSearchParameters<Request> {

    @Schema(description = "Source that emitted the request", example = "CNES")
    private String source;

    @Schema(description = "Session that emitted the request", example = "Today_Mission1")
    private String session;

    @Schema(description = "Name of the Worker type matching the Request Content Type", example = "WorkerAcceptingContentType1")
    private String dispatchedWorkerType;

    @Valid
    @Schema(description = "Filter on Content types")
    private ValuesRestriction<String> contentTypes;

    @Valid
    @Schema(description = "Filter on Request statuses")
    private ValuesRestriction<RequestStatus> statuses;

    @Valid
    @Schema(description = "Filter on Request IDs")
    private ValuesRestriction<Long> ids;

    @Valid
    @Schema(description = "Request creation date restriction")
    private DatesRangeRestriction creationDate = new DatesRangeRestriction();

    public String getSource() {
        return source;
    }

    public static SearchRequestParameters build() {
        return new SearchRequestParameters()
                .withContentTypesExcluded()
                .withStatusesExcluded()
                .withIdsExcluded();
    }

    public SearchRequestParameters withSource(String source) {
        this.source = source;
        return this;
    }

    public String getSession() {
        return session;
    }

    public SearchRequestParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public String getDispatchedWorkerType() {
        return dispatchedWorkerType;
    }

    public SearchRequestParameters withDispatchedWorkerType(String dispatchedWorkerType) {
        this.dispatchedWorkerType = dispatchedWorkerType;
        return this;
    }

    public ValuesRestriction<String> getContentTypes() {
        return contentTypes;
    }

    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    public SearchRequestParameters withCreationDateBefore(OffsetDateTime before) {
        this.creationDate = DatesRangeRestriction.buildBefore(before);
        return this;
    }

    public SearchRequestParameters withCreationDateAfter(OffsetDateTime after) {
        this.creationDate = DatesRangeRestriction.buildAfter(after);
        return this;
    }

    public SearchRequestParameters withCreateDateBeforeAndAfter(OffsetDateTime before, OffsetDateTime after) {
        this.creationDate = DatesRangeRestriction.buildBeforeAndAfter(before, after);
        return this;
    }

    public ValuesRestriction<Long> getIds() { return this.ids; }

    public SearchRequestParameters withIdsIncluded(Long ...ids) {
        this.ids = new ValuesRestriction<Long>().withInclude(Arrays.asList(ids));
        return this;
    }

    public SearchRequestParameters withIdsIncluded(Collection<Long> ids) {
        this.ids = new ValuesRestriction<Long>().withInclude(ids);
        return this;
    }

    public SearchRequestParameters withIdsExcluded(Long ...ids) {
        this.ids = new ValuesRestriction<Long>().withExclude(Arrays.asList(ids));
        return this;
    }

    public SearchRequestParameters withIdsExcluded(Collection<Long> ids) {
        this.ids = new ValuesRestriction<Long>().withExclude(ids);
        return this;
    }

    public ValuesRestriction<RequestStatus> getStatuses() { return this.statuses; }

    public SearchRequestParameters withStatusesIncluded(RequestStatus ...status) {
        this.statuses = new ValuesRestriction<RequestStatus>().withInclude(Arrays.asList(status));
        return this;
    }

    public SearchRequestParameters withStatusesIncluded(Collection<RequestStatus> status) {
        this.statuses = new ValuesRestriction<RequestStatus>().withInclude(status);
        return this;
    }

    public SearchRequestParameters withStatusesExcluded(RequestStatus ...status) {
        this.statuses = new ValuesRestriction<RequestStatus>().withExclude(Arrays.asList(status));
        return this;
    }

    public SearchRequestParameters withStatusesExcluded(Collection<RequestStatus> status) {
        this.statuses = new ValuesRestriction<RequestStatus>().withExclude(status);
        return this;
    }

    public SearchRequestParameters withContentTypesIncluded(String ...contentTypes) {
        this.contentTypes = new ValuesRestriction<String>().withInclude(Arrays.asList(contentTypes));
        return this;
    }

    public SearchRequestParameters withContentTypesIncluded(Collection<String> contentTypes) {
        this.contentTypes = new ValuesRestriction<String>().withInclude(contentTypes);
        return this;
    }

    public SearchRequestParameters withContentTypesExcluded(String ...contentTypes) {
        this.contentTypes = new ValuesRestriction<String>().withExclude(Arrays.asList(contentTypes));
        return this;
    }

    public SearchRequestParameters withContentTypesExcluded(Collection<String> contentTypes) {
        this.contentTypes = new ValuesRestriction<String>().withExclude(contentTypes);
        return this;
    }
}
