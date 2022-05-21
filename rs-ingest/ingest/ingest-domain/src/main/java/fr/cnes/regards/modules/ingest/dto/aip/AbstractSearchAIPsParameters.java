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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.dto.request.SearchSelectionMode;
import org.apache.commons.compress.utils.Lists;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Store AIP criteria filters to do some research against AIP repo
 *
 * @author sbinda
 * @author Léo Mieulet
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSearchAIPsParameters<T extends AbstractSearchAIPsParameters<?>> {

    private AIPState state;

    private EntityType ipType;

    private OAISDateRange lastUpdate = new OAISDateRange();

    private Set<String> providerIds = Sets.newHashSet();

    private String sessionOwner;

    private String session;

    private Set<String> storages = Sets.newHashSet();

    private Set<String> categories = Sets.newHashSet();

    private List<String> tags = Lists.newArrayList();

    private Boolean last = null;

    /**
     * This attribute describe the {@link #aipIds} meaning of that list
     */
    private SearchSelectionMode selectionMode = SearchSelectionMode.INCLUDE;

    /**
     * URN of the AIP(s) to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    private List<String> aipIds = new ArrayList<>();

    public T withState(AIPState state) {
        this.state = state;
        return (T) this;
    }

    public T withLastUpdateFrom(OffsetDateTime from) {
        this.lastUpdate.setFrom(from);
        return (T) this;
    }

    public T withLastUpdateTo(OffsetDateTime to) {
        this.lastUpdate.setTo(to);
        return (T) this;
    }

    public T withTag(String tag) {
        this.tags.add(tag);
        return (T) this;
    }

    public T withTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
        return (T) this;
    }

    public T withTags(Collection<String> tags) {
        if ((tags != null) && !tags.isEmpty()) {
            this.tags.addAll(tags);
        }
        return (T) this;
    }

    public T withProviderId(String providerId) {
        this.providerIds.add(providerId);
        return (T) this;
    }

    public T withProviderIds(String... providerIds) {
        this.providerIds.addAll(Arrays.asList(providerIds));
        return (T) this;
    }

    public T withProviderIds(Collection<String> providerIds) {
        if ((providerIds != null) && !providerIds.isEmpty()) {
            this.providerIds.addAll(providerIds);
        }
        return (T) this;
    }

    public T withSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
        return (T) this;
    }

    public T withSession(String session) {
        this.session = session;
        return (T) this;
    }

    public T withStorage(String storage) {
        this.storages.add(storage);
        return (T) this;
    }

    public T withStorages(String... storages) {
        this.storages.addAll(Arrays.asList(storages));
        return (T) this;
    }

    public T withStorages(Collection<String> storages) {
        if ((storages != null) && !storages.isEmpty()) {
            this.storages.addAll(storages);
        }
        return (T) this;
    }

    public T withCategory(String category) {
        this.categories.add(category);
        return (T) this;
    }

    public T withCategories(String... categories) {
        this.categories.addAll(Arrays.asList(categories));
        return (T) this;
    }

    public T withCategories(Collection<String> categories) {
        if ((categories != null) && !categories.isEmpty()) {
            this.categories.addAll(categories);
        }
        return (T) this;
    }

    public T withAipId(String aipId) {
        this.aipIds.add(aipId);
        return (T) this;
    }

    public T withAipIds(String... aipIds) {
        this.aipIds.addAll(Arrays.asList(aipIds));
        return (T) this;
    }

    public T withAipIds(Collection<String> aipIds) {
        if ((aipIds != null) && !aipIds.isEmpty()) {
            this.aipIds.addAll(aipIds);
        }
        return (T) this;
    }

    public T withSelectionMode(SearchSelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        return (T) this;
    }

    public T withIpType(EntityType ipType) {
        this.ipType = ipType;
        return (T) this;
    }

    public T withLast(Boolean last) {
        this.last = last;
        return (T) this;
    }

    public Boolean getLast() {
        return last;
    }

    public void setLast(Boolean last) {
        this.last = last;
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

    public SearchSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(SearchSelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    public List<String> getAipIds() {
        return aipIds;
    }

    public void setAipIds(List<String> aipIds) {
        this.aipIds = aipIds;
    }

    public EntityType getIpType() {
        return ipType;
    }

    public void setIpType(EntityType ipType) {
        this.ipType = ipType;
    }
}
