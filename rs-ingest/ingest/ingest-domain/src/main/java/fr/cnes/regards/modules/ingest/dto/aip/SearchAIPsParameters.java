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
import java.util.Collection;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import fr.cnes.regards.modules.ingest.domain.aip.AIPState;

/**
 * @author sbinda
 *
 */
public class SearchAIPsParameters {

    private AIPState state;

    private OffsetDateTime from;

    private OffsetDateTime to;

    private List<String> tags = Lists.newArrayList();

    private String providerId;

    private String sessionOwner;

    private String session;

    private List<String> storages = Lists.newArrayList();

    private List<String> categories = Lists.newArrayList();

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
        for (String tag : tags) {
            this.tags.add(tag);
        }
        return this;
    }

    public SearchAIPsParameters withTags(Collection<String> tags) {
        this.tags.addAll(tags);
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
        for (String storage : storages) {
            this.storages.add(storage);
        }
        return this;
    }

    public SearchAIPsParameters withStorages(Collection<String> storages) {
        this.storages.addAll(storages);
        return this;
    }

    public SearchAIPsParameters withCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public SearchAIPsParameters withCategories(String... categories) {
        for (String category : categories) {
            this.categories.add(category);
        }
        return this;
    }

    public SearchAIPsParameters withCategories(Collection<String> categories) {
        this.categories.addAll(categories);
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

    public List<String> getStorages() {
        return storages;
    }

    public void setStorages(List<String> storages) {
        this.storages = storages;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

}
