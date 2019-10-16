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
import org.springframework.format.annotation.DateTimeFormat;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.ingest.domain.aip.AIPState;

/**
 * @author sbinda
 *
 */
public class SearchAIPsParameters {

    private AIPState state;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime to;

    private List<String> tags = Lists.newArrayList();

    private String providerId;

    private String sessionOwner;

    private String session;

    private Set<String> storages = Sets.newHashSet();

    private Set<String> categories = Sets.newHashSet();

    public static SearchAIPsParameters build() {
        return new SearchAIPsParameters();
    }

    public SearchAIPsParameters withState(AIPState state) {
        this.state = state;
        return this;
    }

    public SearchAIPsParameters withFrom(OffsetDateTime from) {
        this.from = from;
        return this;
    }

    public SearchAIPsParameters withTo(OffsetDateTime to) {
        this.to = to;
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
        this.providerId = providerId;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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

}
