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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.aip.OAISDateRange;

/**
 * Store AbstractQuery criteria filters to do some research against AbstractQuery repo
 * @author Léo Mieulet
 */
public class SearchRequestsParameters {

    private Set<String> providerIds = Sets.newHashSet();

    private String sessionOwner;

    private String session;

    private OAISDateRange creationDate = new OAISDateRange();

    private RequestTypeEnum requestType;

    private Set<InternalRequestState> states = new HashSet<>();

    private InternalRequestState stateExcluded;

    private Set<String> requestIds = Sets.newHashSet();

    private SearchSelectionMode requestIdSelectionMode = SearchSelectionMode.INCLUDE;

    public static SearchRequestsParameters build() {
        return new SearchRequestsParameters();
    }

    public SearchRequestsParameters withCreationDateFrom(OffsetDateTime from) {
        this.creationDate.setFrom(from);
        return this;
    }

    public SearchRequestsParameters withCreationDateTo(OffsetDateTime to) {
        this.creationDate.setTo(to);
        return this;
    }

    public SearchRequestsParameters withProviderId(String providerId) {
        this.providerIds.add(providerId);
        return this;
    }

    public SearchRequestsParameters withProviderIds(String... providerIds) {
        this.providerIds.addAll(Arrays.asList(providerIds));
        return this;
    }

    public SearchRequestsParameters withProviderIds(Collection<String> providerIds) {
        if ((providerIds != null) && !providerIds.isEmpty()) {
            this.providerIds.addAll(providerIds);
        }
        return this;
    }

    public SearchRequestsParameters withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return this;
    }

    public SearchRequestsParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public SearchRequestsParameters withRequestType(RequestTypeEnum requestType) {
        this.requestType = requestType;
        return this;
    }

    public SearchRequestsParameters withRequestIds(Set<String> requestIds, SearchSelectionMode requestIdSelectionMode) {
        this.requestIds.addAll(requestIds);
        this.requestIdSelectionMode = requestIdSelectionMode;
        return this;
    }

    public SearchRequestsParameters withState(InternalRequestState state) {
        addState(state);
        return this;
    }

    public OAISDateRange getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OAISDateRange creationDate) {
        this.creationDate = creationDate;
    }

    public Set<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(Set<String> providerIds) {
        this.providerIds = providerIds;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public RequestTypeEnum getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestTypeEnum requestType) {
        this.requestType = requestType;
    }

    public Set<InternalRequestState> getStates() {
        return states;
    }

    public void setStates(Set<InternalRequestState> states) {
        this.states = states;
    }

    public void addState(InternalRequestState state) {
        this.states.add(state);
    }

    public InternalRequestState getStateExcluded() {
        return stateExcluded;
    }

    public void setStateExcluded(InternalRequestState stateExcluded) {
        this.stateExcluded = stateExcluded;
    }

    public Set<String> getRequestIds() {
        return requestIds;
    }

    public void setRequestIds(Set<String> requestIds) {
        this.requestIds = requestIds;
    }

    public SearchSelectionMode getRequestIdSelectionMode() {
        return requestIdSelectionMode;
    }

    public void setRequestIdSelectionMode(SearchSelectionMode requestIdSelectionMode) {
        this.requestIdSelectionMode = requestIdSelectionMode;
    }

}
