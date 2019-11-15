/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.utils.Lists;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.ingest.domain.aip.AIPState;

/**
 * Store AIP criteria filters to do some research against AIP repo
 * @author sbinda
 * @author Léo Mieulet
 */
public class SearchAIPsParameters {

    private AIPState state;

    private OAISDateRange lastUpdate = new OAISDateRange();

    private Set<String> providerIds = Sets.newHashSet();

    private String sessionOwner;

    private String session;

    private Set<String> storages = Sets.newHashSet();

    private Set<String> categories = Sets.newHashSet();

    private List<String> tags = Lists.newArrayList();

    public static SearchAIPsParameters build() {
        return new SearchAIPsParameters();
    }

    public SearchAIPsParameters withState(AIPState state) {
        this.state = state;
        return this;
    }

    public SearchAIPsParameters withLastUpdateFrom(OffsetDateTime from) {
        this.lastUpdate.setFrom(from);
        return this;
    }

    public SearchAIPsParameters withLastUpdateTo(OffsetDateTime to) {
        this.lastUpdate.setTo(to);
        return this;
    }

    public SearchAIPsParameters withTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public SearchAIPsParameters withTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
        return this;
    }

    public SearchAIPsParameters withTags(Collection<String> tags) {
        if ((tags != null) && !tags.isEmpty()) {
            this.tags.addAll(tags);
        }
        return this;
    }

    public SearchAIPsParameters withProviderId(String providerId) {
        this.providerIds.add(providerId);
        return this;
    }

    public SearchAIPsParameters withProviderIds(String... providerIds) {
        this.providerIds.addAll(Arrays.asList(providerIds));
        return this;
    }

    public SearchAIPsParameters withProviderIds(Collection<String> providerIds) {
        if ((providerIds != null) && !providerIds.isEmpty()) {
            this.providerIds.addAll(providerIds);
        }
        return this;
    }

    public SearchAIPsParameters withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return this;
    }

    public SearchAIPsParameters withSession(String session) {
        this.session = session;
        return this;
    }

    public SearchAIPsParameters withStorage(String storage) {
        this.storages.add(storage);
        return this;
    }

    public SearchAIPsParameters withStorages(String... storages) {
        this.storages.addAll(Arrays.asList(storages));
        return this;
    }

    public SearchAIPsParameters withStorages(Collection<String> storages) {
        if ((storages != null) && !storages.isEmpty()) {
            this.storages.addAll(storages);
        }
        return this;
    }

    public SearchAIPsParameters withCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public SearchAIPsParameters withCategories(String... categories) {
        this.categories.addAll(Arrays.asList(categories));
        return this;
    }

    public SearchAIPsParameters withCategories(Collection<String> categories) {
        if ((categories != null) && !categories.isEmpty()) {
            this.categories.addAll(categories);
        }
        return this;
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public Set<String> getStorages() {
        return storages;
    }

    public void setStorages(Set<String> storages) {
        this.storages = storages;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public OAISDateRange getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OAISDateRange lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
