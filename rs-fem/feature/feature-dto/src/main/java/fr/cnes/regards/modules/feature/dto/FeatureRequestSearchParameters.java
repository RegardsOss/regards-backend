/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.dto;

import java.time.OffsetDateTime;

import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * Search parameters to retrieve {@link FeatureRequestDTO}s
 *
 * @author Sébastien Binda
 *
 */
public class FeatureRequestSearchParameters {

    private String source;

    private String session;

    private String providerId;

    private OffsetDateTime start;

    private OffsetDateTime end;

    private RequestState state;

    public static FeatureRequestSearchParameters build() {
        return new FeatureRequestSearchParameters();
    }

    public FeatureRequestSearchParameters withSource(String source) {
        this.setSource(source);
        return this;
    }

    public FeatureRequestSearchParameters withSession(String session) {
        this.setSession(session);
        return this;
    }

    public FeatureRequestSearchParameters withProviderId(String providerId) {
        this.setProviderId(providerId);
        return this;
    }

    public FeatureRequestSearchParameters withStart(OffsetDateTime start) {
        this.setStart(start);
        return this;
    }

    public FeatureRequestSearchParameters withEnd(OffsetDateTime end) {
        this.setEnd(end);
        return this;
    }

    public FeatureRequestSearchParameters withState(RequestState state) {
        this.setState(state);
        return this;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

}
