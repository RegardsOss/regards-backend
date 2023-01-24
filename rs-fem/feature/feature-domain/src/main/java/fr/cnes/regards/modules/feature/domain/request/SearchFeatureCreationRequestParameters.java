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

import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * @author Stephane Cortine
 */
public class SearchFeatureCreationRequestParameters extends SearchFeatureRequestParameters
    implements AbstractSearchParameters<FeatureCreationRequest> {

    public SearchFeatureCreationRequestParameters() {
    }

    public SearchFeatureCreationRequestParameters(SearchFeatureRequestParameters searchFeatureRequestParameters) {
        this.setSource(searchFeatureRequestParameters.getSource());
        this.setSession(searchFeatureRequestParameters.getSession());
        this.setProviderIds(searchFeatureRequestParameters.getProviderIds());
        this.setStates(searchFeatureRequestParameters.getStates());
        this.setLastUpdate(searchFeatureRequestParameters.getLastUpdate());
    }

    public SearchFeatureCreationRequestParameters withSource(String source) {
        setSource(source);
        return this;
    }

    public SearchFeatureCreationRequestParameters withSession(String session) {
        setSession(session);
        return this;
    }

    public SearchFeatureCreationRequestParameters withProviderIdsIncluded(Collection<String> providerIds) {
        this.setProviderIds(new ValuesRestriction<String>().withInclude(providerIds));
        return this;
    }

    public SearchFeatureCreationRequestParameters withProviderIdsExcluded(Collection<String> providerIds) {
        setProviderIds(new ValuesRestriction<String>().withExclude(providerIds));
        return this;
    }

    public SearchFeatureCreationRequestParameters withStatesIncluded(Collection<RequestState> states) {
        setStates(new ValuesRestriction<RequestState>().withInclude(states));
        return this;
    }

    public SearchFeatureCreationRequestParameters withStatesExcluded(Collection<RequestState> states) {
        setStates(new ValuesRestriction<RequestState>().withExclude(states));
        return this;
    }

    public SearchFeatureCreationRequestParameters withLastUpdateBefore(OffsetDateTime before) {
        getLastUpdate().setBefore(before);
        return this;
    }

    public SearchFeatureCreationRequestParameters withLastUpdateAfter(OffsetDateTime after) {
        getLastUpdate().setAfter(after);
        return this;
    }
}