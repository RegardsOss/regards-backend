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

import com.google.common.collect.Lists;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class SearchFacetsAIPsParameters extends SearchAIPsParameters {
    /**
     * list of API id included (optional)
     */
    private Set<String> aipIds = new HashSet<>();

    /**
     * list of API id excluded (optional)
     */
    private Set<String> aipIdsExcluded = new HashSet<>();

    public SearchFacetsAIPsParameters withAIPIds(String aipId) {
        this.aipIds.add(aipId);
        return this;
    }

    public SearchFacetsAIPsParameters withAIPIds(String... aipIds) {
        this.aipIds.addAll(Arrays.asList(aipIds));
        return this;
    }

    public SearchFacetsAIPsParameters withAIPIds(Collection<String> aipIds) {
        this.aipIds.addAll(aipIds);
        return this;
    }

    public SearchFacetsAIPsParameters withAIPIdsExcluded(String aipIdExcluded) {
        this.aipIdsExcluded.add(aipIdExcluded);
        return this;
    }

    public SearchFacetsAIPsParameters withAIPIdsExcluded(String... aipIdsExcluded) {
        this.aipIdsExcluded.addAll(Arrays.asList(aipIdsExcluded));
        return this;
    }

    public SearchFacetsAIPsParameters withAIPIdsExcluded(Collection<String> aipIdsExcluded) {
        this.aipIdsExcluded.addAll(aipIdsExcluded);
        return this;
    }

    /**
     * Delegation methods START
     */

    public static SearchFacetsAIPsParameters build() {
        return new SearchFacetsAIPsParameters();
    }

    public SearchFacetsAIPsParameters withState(AIPState state) {
        super.withState(state);
        return this;
    }

    public SearchFacetsAIPsParameters withTag(String tag) {
        super.withTag(tag);
        return this;
    }

    public SearchFacetsAIPsParameters withTags(String... tags) {
        super.withTags(Lists.newArrayList(tags));
        return this;
    }

    public SearchFacetsAIPsParameters withTags(Collection<String> tags) {
        super.withTags(tags);
        return this;
    }

    public SearchFacetsAIPsParameters withProviderId(String providerId) {
        super.withProviderId(providerId);
        return this;
    }

    public SearchFacetsAIPsParameters withSessionOwner(String sessionOwner) {
        super.withSessionOwner(sessionOwner);
        return this;
    }

    public SearchFacetsAIPsParameters withSession(String session) {
        super.withSession(session);
        return this;
    }

    public SearchFacetsAIPsParameters withStorage(String storage) {
        super.withStorage(storage);
        return this;
    }

    public SearchFacetsAIPsParameters withStorages(String... storages) {
        super.withStorages(Lists.newArrayList(storages));
        return this;
    }

    public SearchFacetsAIPsParameters withStorages(Collection<String> storages) {
        super.withStorages(storages);
        return this;
    }

    public SearchFacetsAIPsParameters withCategory(String category) {
        super.withCategory(category);
        return this;
    }

    public SearchFacetsAIPsParameters withCategories(String... categories) {
        super.withCategories(Arrays.asList(categories));
        return this;
    }

    public SearchFacetsAIPsParameters withCategories(Collection<String> categories) {
        super.withCategories(categories);
        return this;
    }

    public SearchFacetsAIPsParameters withLastUpdateFrom(OffsetDateTime from) {
        super.withLastUpdateFrom(from);
        return this;
    }

    public SearchFacetsAIPsParameters withLastUpdateTo(OffsetDateTime to) {
        super.withLastUpdateTo(to);
        return this;
    }

    public SearchFacetsAIPsParameters withProviderIds(String... providerIds) {
        super.withProviderIds(providerIds);
        return this;
    }

    public SearchFacetsAIPsParameters withProviderIds(Collection<String> providerIds) {
        super.withProviderIds(providerIds);
        return this;
    }

    /**
     * Delegation methods END
     */

    public Set<String> getAipIds() {
        return aipIds;
    }

    public void setAipIds(Set<String> aipIds) {
        this.aipIds = aipIds;
    }

    public Set<String> getAipIdsExcluded() {
        return aipIdsExcluded;
    }

    public void setAipIdsExcluded(Set<String> aipIdsExcluded) {
        this.aipIdsExcluded = aipIdsExcluded;
    }
}
