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
package fr.cnes.regards.modules.ingest.dto.sip;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import org.apache.commons.compress.utils.Lists;
import org.springframework.format.annotation.DateTimeFormat;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;

/**
 * Store SIP criteria filters to do some research against SIP repo
 * @author Léo Mieulet
 */
public class SearchSIPsParameters {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime from;

    private List<SIPState> states = Lists.newArrayList();

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    private String session;

    private String processing;

    private EntityType ipType;

    private Set<String> providerIds = Sets.newHashSet();

    private Set<String> categories = Sets.newHashSet();

    private List<String> tags = Lists.newArrayList();

    public static SearchSIPsParameters build() {
        return new SearchSIPsParameters();
    }

    public SearchSIPsParameters withState(SIPState state) {
        this.states.add(state);
        return this;
    }

    public SearchSIPsParameters withStates(SIPState... states) {
        this.states.addAll(Arrays.asList(states));
        return this;
    }

    public SearchSIPsParameters withStates(Collection<SIPState> states) {
        if ((states != null) && !states.isEmpty()) {
            this.states.addAll(states);
        }
        return this;
    }

    public SearchSIPsParameters withFrom(OffsetDateTime from) {
        this.from = from;
        return this;
    }

    public SearchSIPsParameters withProviderId(String providerId) {
        this.providerIds.add(providerId);
        return this;
    }

    public SearchSIPsParameters withProviderIds(String... providerIds) {
        this.providerIds.addAll(Arrays.asList(providerIds));
        return this;
    }

    public SearchSIPsParameters withProviderIds(Collection<String> providerIds) {
        this.providerIds.addAll(providerIds);
        return this;
    }

    public SearchSIPsParameters withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return this;
    }

    public SearchSIPsParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public SearchSIPsParameters withProcessing(String processing) {
        this.processing = processing;
        return this;
    }

    public SearchSIPsParameters withCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public SearchSIPsParameters withCategories(String... categories) {
        this.categories.addAll(Arrays.asList(categories));
        return this;
    }

    public SearchSIPsParameters withCategories(Collection<String> categories) {
        if ((categories != null) && !categories.isEmpty()) {
            this.categories.addAll(categories);
        }
        return this;
    }

    public SearchSIPsParameters withTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public SearchSIPsParameters withTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
        return this;
    }

    public SearchSIPsParameters withTags(Collection<String> tags) {
        if ((tags != null) && !tags.isEmpty()) {
            this.tags.addAll(tags);
        }
        return this;
    }

    public SearchSIPsParameters withIpType(EntityType ipType) {
        this.ipType = ipType;
        return this;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public void setFrom(OffsetDateTime from) {
        this.from = from;
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

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<SIPState> getStates() {
        return states;
    }

    public void setStates(List<SIPState> states) {
        this.states = states;
    }

    public Set<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(Set<String> providerIds) {
        this.providerIds = providerIds;
    }

    public EntityType getIpType() {
        return ipType;
    }

    public void setIpType(EntityType ipType) {
        this.ipType = ipType;
    }

    public String getProcessing() {
        return processing;
    }

    public void setProcessing(String processing) {
        this.processing = processing;
    }
}
