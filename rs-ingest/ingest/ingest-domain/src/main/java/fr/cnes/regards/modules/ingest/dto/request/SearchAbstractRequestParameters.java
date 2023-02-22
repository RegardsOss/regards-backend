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
package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * This is criterias for filter on entity abstract request
 *
 * @author Stephane Cortine
 */
public class SearchAbstractRequestParameters implements AbstractSearchParameters {

    @Schema(description = "Filter on request id")
    private ValuesRestriction<Long> requestIds;

    @Schema(description = "Filter on provider id")
    private ValuesRestriction<String> providerIds;

    @Schema(description = "Filter on owner of session that emitted the request")
    private String sessionOwner;

    @Schema(description = "Filter on session that emitted the request", example = "Today_Mission1")
    private String session;

    @Schema(description = "Filter on creation date")
    private DatesRangeRestriction creationDate = new DatesRangeRestriction();

    @Schema(description = "List of internal request states",
            example = "TO_SCHEDULE|CREATED|WAITING_VERSIONING_MODE|BLOCKED|RUNNING|ERROR|ABORTED|IGNORED")
    private ValuesRestriction<InternalRequestState> requestStates;

    @Schema(description = "List of request type",
            example = "INGEST|UPDATE|AIP_UPDATES_CREATOR|AIP_SAVE_METADATA|AIP_POST_PROCESS|OAIS_DELETION|ABORTED|OAIS_DELETION_CREATOR")
    private ValuesRestriction<RequestTypeEnum> requestIpTypes;

    public ValuesRestriction<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(ValuesRestriction<String> providerIds) {
        this.providerIds = providerIds;
    }

    public SearchAbstractRequestParameters withProviderIdsIncluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withInclude(providerIds);
        return this;
    }

    public SearchAbstractRequestParameters withProviderIdsExcluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withExclude(providerIds);
        return this;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public SearchAbstractRequestParameters withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return this;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SearchAbstractRequestParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DatesRangeRestriction creationDate) {
        this.creationDate = creationDate;
    }

    public SearchAbstractRequestParameters withLastUpdateAfter(OffsetDateTime after) {
        this.creationDate.setAfter(after);
        return this;
    }

    public SearchAbstractRequestParameters withLastUpdateBefore(OffsetDateTime before) {
        this.creationDate.setBefore(before);
        return this;
    }

    public ValuesRestriction<InternalRequestState> getRequestStates() {
        return requestStates;
    }

    public void setRequestStates(ValuesRestriction<InternalRequestState> requestStates) {
        this.requestStates = requestStates;
    }

    public SearchAbstractRequestParameters withRequestStatesIncluded(Collection<InternalRequestState> states) {
        this.requestStates = new ValuesRestriction<InternalRequestState>().withInclude(states);
        return this;
    }

    public SearchAbstractRequestParameters withRequestStatesExcluded(Collection<InternalRequestState> states) {
        this.requestStates = new ValuesRestriction<InternalRequestState>().withExclude(states);
        return this;
    }

    public ValuesRestriction<RequestTypeEnum> getRequestIpTypes() {
        return requestIpTypes;
    }

    public void setRequestIpTypes(ValuesRestriction<RequestTypeEnum> requestIpTypes) {
        this.requestIpTypes = requestIpTypes;
    }

    public SearchAbstractRequestParameters withRequestIpTypesIncluded(Collection<RequestTypeEnum> requestTypes) {
        this.requestIpTypes = new ValuesRestriction<RequestTypeEnum>().withInclude(requestTypes);
        return this;
    }

    public SearchAbstractRequestParameters withRequestIpTypesExcluded(Collection<RequestTypeEnum> requestTypes) {
        this.requestIpTypes = new ValuesRestriction<RequestTypeEnum>().withExclude(requestTypes);
        return this;
    }

    public ValuesRestriction<Long> getRequestIds() {
        return requestIds;
    }

    public void setRequestIds(ValuesRestriction<Long> requestIds) {
        this.requestIds = requestIds;
    }

    public SearchAbstractRequestParameters withRequestIdsIncluded(Collection<Long> requestIds) {
        this.requestIds = new ValuesRestriction<Long>().withInclude(requestIds);
        return this;
    }

    public SearchAbstractRequestParameters withRequestIdsExcluded(Collection<Long> requestIds) {
        this.requestIds = new ValuesRestriction<Long>().withExclude(requestIds);
        return this;
    }

}
