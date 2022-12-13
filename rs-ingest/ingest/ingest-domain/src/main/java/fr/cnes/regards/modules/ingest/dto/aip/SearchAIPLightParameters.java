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
package fr.cnes.regards.modules.ingest.dto.aip;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * This is criterias for filter on entity light AIP
 *
 * @author Stephane Cortine
 */
public class SearchAIPLightParameters implements AbstractSearchParameters<AIPEntityLight> {

    @Schema(description = "Filter on state of aip", example = "GENERATED|STORED|DELETED")
    private ValuesRestriction<AIPState> aipStates;

    @Schema(description = "Filter on types of entity", example = "COLLECTION|DATA|DATASET")
    private ValuesRestriction<EntityType> aipIpTypes;

    @Schema(description = "Filter on range of date for last update")
    private DatesRangeRestriction lastUpdate = new DatesRangeRestriction();

    @Schema(description = "Filter on provider id")
    private ValuesRestriction<String> providerIds;

    @Schema(description = "Filter on owner of session that emitted the request")
    private String sessionOwner;

    @Schema(description = "Filter on session that emitted the request", example = "Today_Mission1")
    private String session;

    @Schema(description = "Filter on storages")
    private ValuesRestriction<String> storages;

    @Schema(description = "Filter on categories")
    private ValuesRestriction<String> categories;

    @Schema(description = "Filter on tags")
    private ValuesRestriction<String> tags;

    @Schema(description = "Filter on last")
    private Boolean last = null;

    /**
     * URN of the AIP(s) to preserve or remove in the specified session
     */
    @Schema(description = "Filter on identifier of aip")
    private ValuesRestriction<String> aipIds;

    public ValuesRestriction<AIPState> getAipStates() {
        return aipStates;
    }

    public void setAipStates(ValuesRestriction<AIPState> aipStates) {
        this.aipStates = aipStates;
    }

    public SearchAIPLightParameters withStatesIncluded(Collection<AIPState> states) {
        this.aipStates = new ValuesRestriction<AIPState>().withInclude(states);
        return this;
    }

    public SearchAIPLightParameters withStatesExcluded(Collection<AIPState> states) {
        this.aipStates = new ValuesRestriction<AIPState>().withExclude(states);
        return this;
    }

    public ValuesRestriction<EntityType> getAipIpTypes() {
        return aipIpTypes;
    }

    public void setAipIpType(ValuesRestriction<EntityType> aipIpType) {
        this.aipIpTypes = aipIpTypes;
    }

    public SearchAIPLightParameters withAipIpType(Collection<EntityType> aipIpTypes) {
        this.aipIpTypes = new ValuesRestriction<EntityType>().withInclude(aipIpTypes);
        return this;
    }

    public DatesRangeRestriction getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(DatesRangeRestriction lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public SearchAIPLightParameters withLastUpdateAfter(OffsetDateTime after) {
        this.lastUpdate.setAfter(after);
        return this;
    }

    public SearchAIPLightParameters withLastUpdateBefore(OffsetDateTime before) {
        this.lastUpdate.setBefore(before);
        return this;
    }

    public ValuesRestriction<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(ValuesRestriction<String> providerIds) {
        this.providerIds = providerIds;
    }

    public SearchAIPLightParameters withProviderIdsIncluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withInclude(providerIds);
        return this;
    }

    public SearchAIPLightParameters withProviderIdsExcluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withExclude(providerIds);
        return this;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public SearchAIPLightParameters withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return this;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SearchAIPLightParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public ValuesRestriction<String> getStorages() {
        return storages;
    }

    public void setStorages(ValuesRestriction<String> storages) {
        this.storages = storages;
    }

    public SearchAIPLightParameters withStoragesIncluded(Collection<String> storages) {
        this.storages = new ValuesRestriction<String>().withInclude(storages);
        return this;
    }

    public SearchAIPLightParameters withStoragesExcluded(Collection<String> storages) {
        this.storages = new ValuesRestriction<String>().withExclude(storages);
        return this;
    }

    public ValuesRestriction<String> getCategories() {
        return categories;
    }

    public void setCategories(ValuesRestriction<String> categories) {
        this.categories = categories;
    }

    public SearchAIPLightParameters withCategoriesIncluded(Collection<String> categories) {
        this.categories = new ValuesRestriction<String>().withInclude(categories);
        return this;
    }

    public SearchAIPLightParameters withCategoriesExcluded(Collection<String> categories) {
        this.categories = new ValuesRestriction<String>().withExclude(categories);
        return this;
    }

    public ValuesRestriction<String> getTags() {
        return tags;
    }

    public void setTags(ValuesRestriction<String> tags) {
        this.tags = tags;
    }

    public SearchAIPLightParameters withTagsExcluded(Collection<String> tags) {
        this.tags = new ValuesRestriction<String>().withExclude(tags);
        return this;
    }

    public SearchAIPLightParameters withTagsIncluded(Collection<String> tags) {
        this.tags = new ValuesRestriction<String>().withInclude(tags);
        return this;
    }

    public Boolean getLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
    }

    public SearchAIPLightParameters withLast(Boolean last) {
        this.last = last;
        return this;
    }

    public ValuesRestriction<String> getAipIds() {
        return aipIds;
    }

    public void setAipIds(ValuesRestriction<String> aipIds) {
        this.aipIds = aipIds;
    }

    public SearchAIPLightParameters withAipIdsExcluded(Collection<String> aipIds) {
        this.aipIds = new ValuesRestriction<String>().withExclude(aipIds);
        return this;
    }

    public SearchAIPLightParameters withAipIdsIncluded(Collection<String> aipIds) {
        this.aipIds = new ValuesRestriction<String>().withInclude(aipIds);
        return this;
    }
}
