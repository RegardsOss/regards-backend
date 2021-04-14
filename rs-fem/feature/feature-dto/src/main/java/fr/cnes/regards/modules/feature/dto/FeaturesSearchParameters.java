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

/**
 * POJO to define a selection filters for {@link FeatureEntityDto}
 *
 * @author Sébastien Binda
 *
 */
public class FeaturesSearchParameters {

    private String model;

    private String providerId;

    private String source;

    private String session;

    private OffsetDateTime from;

    private OffsetDateTime to;

    public static FeaturesSearchParameters build() {
        return new FeaturesSearchParameters();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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

    public OffsetDateTime getFrom() {
        return from;
    }

    public void setFrom(OffsetDateTime from) {
        this.from = from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public void setTo(OffsetDateTime to) {
        this.to = to;
    }

    public FeaturesSearchParameters withModel(String model) {
        this.setModel(model);
        return this;
    }

    public FeaturesSearchParameters withSource(String source) {
        this.setSource(source);
        return this;
    }

    public FeaturesSearchParameters withSession(String session) {
        this.setSession(session);
        return this;
    }

    public FeaturesSearchParameters withProviderId(String providerId) {
        this.setProviderId(providerId);
        return this;
    }

    public FeaturesSearchParameters withFrom(OffsetDateTime from) {
        this.setFrom(from);
        return this;
    }

    public FeaturesSearchParameters withTo(OffsetDateTime to) {
        this.setTo(to);
        return this;
    }

}
