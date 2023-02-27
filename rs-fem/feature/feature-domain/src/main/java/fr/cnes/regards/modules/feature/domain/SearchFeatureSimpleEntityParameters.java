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
package fr.cnes.regards.modules.feature.domain;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * @author Stephane Cortine
 */
public class SearchFeatureSimpleEntityParameters implements AbstractSearchParameters {

    @Schema(description = "Filter on feature id")
    private ValuesRestriction<Long> featureIds;

    @Schema(description = "Filter on model")
    private String model;

    @Schema(description = "Filter on provider id")
    private ValuesRestriction<String> providerIds;

    @Schema(description = "Filter on source")
    private String source;

    @Schema(description = "Filter on session that emitted the request", example = "Today_Mission1")
    private String session;

    @Schema(description = "Filter on range of date for last update")
    private DatesRangeRestriction lastUpdate = new DatesRangeRestriction();

    @Schema(description = "Filter on dissemination pending", example = "TRUE|FALSE")
    private Boolean disseminationPending;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public SearchFeatureSimpleEntityParameters withModel(String model) {
        this.model = model;
        return this;
    }

    public ValuesRestriction<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(ValuesRestriction<String> providerIds) {
        this.providerIds = providerIds;
    }

    public SearchFeatureSimpleEntityParameters withProviderIdsIncluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withInclude(providerIds)
                                                          .withMatchMode(ValuesRestrictionMatchMode.STARTS_WITH)
                                                          .withIgnoreCase(true);
        return this;
    }

    public SearchFeatureSimpleEntityParameters withProviderIdsExcluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withExclude(providerIds)
                                                          .withMatchMode(ValuesRestrictionMatchMode.STARTS_WITH)
                                                          .withIgnoreCase(true);
        return this;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public SearchFeatureSimpleEntityParameters withSource(String source) {
        this.source = source;
        return this;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SearchFeatureSimpleEntityParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public DatesRangeRestriction getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(DatesRangeRestriction lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public SearchFeatureSimpleEntityParameters withLastUpdateBefore(OffsetDateTime before) {
        this.lastUpdate.setBefore(before);
        return this;
    }

    public SearchFeatureSimpleEntityParameters withLastUpdateAfter(OffsetDateTime after) {
        this.lastUpdate.setAfter(after);
        return this;
    }

    public Boolean getDisseminationPending() {
        return disseminationPending;
    }

    public void setDisseminationPending(Boolean disseminationPending) {
        if (disseminationPending == null) {
            disseminationPending = Boolean.FALSE;
        }
        this.disseminationPending = disseminationPending;
    }

    public SearchFeatureSimpleEntityParameters withDisseminationPending(Boolean disseminationPending) {
        setDisseminationPending(disseminationPending);
        return this;
    }

    public ValuesRestriction<Long> getFeatureIds() {
        return featureIds;
    }

    public void setFeatureIds(ValuesRestriction<Long> featureIds) {
        this.featureIds = featureIds;
    }

    public SearchFeatureSimpleEntityParameters withFeatureIdsIncluded(Collection<Long> featureIds) {
        this.featureIds = new ValuesRestriction<>(featureIds, ValuesRestrictionMode.INCLUDE);
        return this;
    }

    public SearchFeatureSimpleEntityParameters withFeatureIdsExcluded(Collection<Long> featureIds) {
        this.featureIds = new ValuesRestriction<>(featureIds, ValuesRestrictionMode.EXCLUDE);
        return this;
    }
}
