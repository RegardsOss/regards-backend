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
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * Concrete class that stores AIP criteria to do some research against AIP repository
 *
 * @author sbinda
 * @author Léo Mieulet
 */
public class SearchAIPsParameters implements AbstractSearchParameters {

    @Valid
    @Schema(description = "Filter on state of aip")
    private ValuesRestriction<AIPState> aipStates;

    @Valid
    @Schema(description = "Filter on types of entity")
    private ValuesRestriction<EntityType> aipIpTypes;

    @Schema(description = "Filter on range of date for last update")
    private DatesRangeRestriction lastUpdate = new DatesRangeRestriction();

    @Schema(description = "Filter on range of date for creation date")
    private DatesRangeRestriction creationDate = new DatesRangeRestriction();

    @Valid
    @Schema(description = "Filter on provider id")
    private ValuesRestriction<String> providerIds;

    @Schema(description = "Filter on owner of session that emitted the request")
    private String sessionOwner;

    @Schema(description = "Filter on session that emitted the request", example = "Today_Mission1")
    private String session;

    @Valid
    @Schema(description = "Filter on storages")
    private ValuesRestriction<String> storages;

    @Valid
    @Schema(description = "Filter on categories")
    private ValuesRestriction<String> categories;

    @Valid
    @Schema(description = "Filter on tags")
    private ValuesRestriction<String> tags;

    @Schema(description = "Filter on last")
    private Boolean last = null;

    @Valid
    @Schema(description = "Filter on status of the dissemination")
    private ValuesRestriction<DisseminationStatus> disseminationStatus;

    /**
     * URN of the AIP(s) to preserve or remove in the specified session
     */
    @Valid
    @Schema(description = "Filter on identifier of aip")
    private ValuesRestriction<String> aipIds;

    private String originUrn;

    public ValuesRestriction<AIPState> getAipStates() {
        return aipStates;
    }

    public void setAipStates(ValuesRestriction<AIPState> aipStates) {
        this.aipStates = aipStates;
    }

    public SearchAIPsParameters withStatesIncluded(Collection<AIPState> states) {
        this.aipStates = new ValuesRestriction<AIPState>().withInclude(states);
        return this;
    }

    public SearchAIPsParameters withStatesExcluded(Collection<AIPState> states) {
        this.aipStates = new ValuesRestriction<AIPState>().withExclude(states);
        return this;
    }

    public ValuesRestriction<EntityType> getAipIpTypes() {
        return aipIpTypes;
    }

    public void setAipIpTypes(ValuesRestriction<EntityType> aipIpTypes) {
        this.aipIpTypes = aipIpTypes;
    }

    public SearchAIPsParameters withAipIpType(Collection<EntityType> aipIpTypes) {
        this.aipIpTypes = new ValuesRestriction<EntityType>().withInclude(aipIpTypes);
        return this;
    }

    public DatesRangeRestriction getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(DatesRangeRestriction lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public SearchAIPsParameters withLastUpdateAfter(OffsetDateTime after) {
        this.lastUpdate.setAfter(after);
        return this;
    }

    public SearchAIPsParameters withLastUpdateBefore(OffsetDateTime before) {
        this.lastUpdate.setBefore(before);
        return this;
    }

    public ValuesRestriction<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(ValuesRestriction<String> providerIds) {
        this.providerIds = providerIds;
    }

    public SearchAIPsParameters withProviderIdsIncluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withInclude(providerIds);
        return this;
    }

    public SearchAIPsParameters withProviderIdsIncludedLike(Collection<String> providerIds,
                                                            ValuesRestrictionMatchMode matchMode,
                                                            boolean ignoreCase) {
        this.providerIds = new ValuesRestriction<String>().withInclude(providerIds)
                                                          .withMatchMode(matchMode)
                                                          .withIgnoreCase(ignoreCase);
        return this;
    }

    public SearchAIPsParameters withProviderIdsExcludedLike(Collection<String> providerIds,
                                                            ValuesRestrictionMatchMode matchMode,
                                                            boolean ignoreCase) {
        this.providerIds = new ValuesRestriction<String>().withExclude(providerIds)
                                                          .withMatchMode(matchMode)
                                                          .withIgnoreCase(ignoreCase);
        return this;
    }

    public SearchAIPsParameters withProviderIdsExcluded(Collection<String> providerIds) {
        this.providerIds = new ValuesRestriction<String>().withExclude(providerIds);
        return this;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public SearchAIPsParameters withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return this;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public SearchAIPsParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public ValuesRestriction<String> getStorages() {
        return storages;
    }

    public void setStorages(ValuesRestriction<String> storages) {
        this.storages = storages;
    }

    public SearchAIPsParameters withStoragesIncluded(Collection<String> storages) {
        this.storages = new ValuesRestriction<String>().withInclude(storages);
        return this;
    }

    public SearchAIPsParameters withStoragesExcluded(Collection<String> storages) {
        this.storages = new ValuesRestriction<String>().withExclude(storages);
        return this;
    }

    public ValuesRestriction<String> getCategories() {
        return categories;
    }

    public void setCategories(ValuesRestriction<String> categories) {
        this.categories = categories;
    }

    public SearchAIPsParameters withCategoriesIncluded(Collection<String> categories) {
        this.categories = new ValuesRestriction<String>().withInclude(categories);
        return this;
    }

    public SearchAIPsParameters withCategoriesExcluded(Collection<String> categories) {
        this.categories = new ValuesRestriction<String>().withExclude(categories);
        return this;
    }

    public ValuesRestriction<String> getTags() {
        return tags;
    }

    public void setTags(ValuesRestriction<String> tags) {
        this.tags = tags;
    }

    public SearchAIPsParameters withTagsExcluded(Collection<String> tags) {
        this.tags = new ValuesRestriction<String>().withExclude(tags);
        return this;
    }

    public SearchAIPsParameters withTagsIncluded(Collection<String> tags) {
        this.tags = new ValuesRestriction<String>().withInclude(tags);
        return this;
    }

    public SearchAIPsParameters withOriginUrn(String originUrn) {
        this.originUrn = originUrn;
        return this;
    }

    public String getOriginUrn() {
        return this.originUrn;
    }

    public Boolean getLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
    }

    public SearchAIPsParameters withLast(Boolean last) {
        this.last = last;
        return this;
    }

    public ValuesRestriction<String> getAipIds() {
        return aipIds;
    }

    public void setAipIds(ValuesRestriction<String> aipIds) {
        this.aipIds = aipIds;
    }

    public SearchAIPsParameters withAipIdsExcluded(Collection<String> aipIds) {
        this.aipIds = new ValuesRestriction<String>().withExclude(aipIds);
        return this;
    }

    public SearchAIPsParameters withAipIdsIncluded(Collection<String> aipIds) {
        this.aipIds = new ValuesRestriction<String>().withInclude(aipIds);
        return this;
    }

    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DatesRangeRestriction creationDate) {
        this.creationDate = creationDate;
    }

    public SearchAIPsParameters withCreationDateAfter(OffsetDateTime after) {
        this.creationDate.setAfter(after);
        return this;
    }

    public SearchAIPsParameters withCreationDateBefore(OffsetDateTime before) {
        this.creationDate.setBefore(before);
        return this;
    }

    public ValuesRestriction<DisseminationStatus> getDisseminationStatus() {
        return disseminationStatus;
    }

    public void setDisseminationStatus(ValuesRestriction<DisseminationStatus> status) {
        this.disseminationStatus = status;
    }

    public SearchAIPsParameters withDisseminationStatusIncluded(Collection<DisseminationStatus> status) {
        this.disseminationStatus = new ValuesRestriction<DisseminationStatus>().withInclude(status);
        return this;
    }

    public SearchAIPsParameters withDisseminationStatusExcluded(Collection<DisseminationStatus> status) {
        this.disseminationStatus = new ValuesRestriction<DisseminationStatus>().withExclude(status);
        return this;
    }
}
