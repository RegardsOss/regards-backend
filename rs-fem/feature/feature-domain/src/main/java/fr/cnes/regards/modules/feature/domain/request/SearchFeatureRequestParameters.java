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
package fr.cnes.regards.modules.feature.domain.request;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * @author Stephane Cortine
 */
public class SearchFeatureRequestParameters {

    @Schema(description = "Filter on source")
    private String source;

    @Schema(description = "Filter on session that emitted the request", example = "Today_Mission1")
    private String session;

    @Schema(description = "Filter on provider id")
    private ValuesRestriction<String> providerIds;

    @Schema(description = "Filter on request states", example = "GRANTED|DENIED|SUCCESS|ERROR")
    private ValuesRestriction<RequestState> states;

    @Schema(description = "Filter on range of date for last update")
    private DatesRangeRestriction lastUpdate = new DatesRangeRestriction();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public SearchFeatureRequestParameters withSource(String source) {
        setSource(source);
        return this;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SearchFeatureRequestParameters withSession(String session) {
        setSession(session);
        return this;
    }

    public ValuesRestriction<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(ValuesRestriction<String> providerIds) {
        this.providerIds = providerIds;
    }

    public SearchFeatureRequestParameters withProviderIdsIncluded(Collection<String> providerIds) {
        this.setProviderIds(new ValuesRestriction<String>().withInclude(providerIds));
        return this;
    }

    public SearchFeatureRequestParameters withProviderIdsExcluded(Collection<String> providerIds) {
        setProviderIds(new ValuesRestriction<String>().withExclude(providerIds));
        return this;
    }

    public ValuesRestriction<RequestState> getStates() {
        return states;
    }

    public void setStates(ValuesRestriction<RequestState> states) {
        this.states = states;
    }

    public SearchFeatureRequestParameters withStatesIncluded(Collection<RequestState> states) {
        setStates(new ValuesRestriction<RequestState>().withInclude(states));
        return this;
    }

    public SearchFeatureRequestParameters withStatesExcluded(Collection<RequestState> states) {
        setStates(new ValuesRestriction<RequestState>().withExclude(states));
        return this;
    }

    public DatesRangeRestriction getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(DatesRangeRestriction lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public SearchFeatureRequestParameters withLastUpdateBefore(OffsetDateTime before) {
        getLastUpdate().setBefore(before);
        return this;
    }

    public SearchFeatureRequestParameters withLastUpdateAfter(OffsetDateTime after) {
        getLastUpdate().setAfter(after);
        return this;
    }

}
