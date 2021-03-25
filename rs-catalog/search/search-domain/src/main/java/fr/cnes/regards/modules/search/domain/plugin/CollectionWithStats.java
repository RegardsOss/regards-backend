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
package fr.cnes.regards.modules.search.domain.plugin;

import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import org.elasticsearch.search.aggregations.Aggregation;

import java.util.List;

/**
 * This POJO wrap stac collection informations found with URN and DataObjects' aggregation data contained in this collection
 */
public final class CollectionWithStats {
    private final AbstractEntity collection;
    private final List<Aggregation> aggregationList;

    public CollectionWithStats(AbstractEntity collection, List<Aggregation> aggregationList) {
        this.collection = collection;
        this.aggregationList = aggregationList;
    }

    public AbstractEntity getCollection() {
        return this.collection;
    }

    public List<Aggregation> getAggregationList() {
        return this.aggregationList;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CollectionWithStats)) return false;
        final CollectionWithStats other = (CollectionWithStats) o;
        final Object this$collection = this.getCollection();
        final Object other$collection = other.getCollection();
        if (this$collection == null ? other$collection != null : !this$collection.equals(other$collection))
            return false;
        final Object this$aggregationList = this.getAggregationList();
        final Object other$aggregationList = other.getAggregationList();
        if (this$aggregationList == null ? other$aggregationList != null : !this$aggregationList.equals(other$aggregationList))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $collection = this.getCollection();
        result = result * PRIME + ($collection == null ? 43 : $collection.hashCode());
        final Object $aggregationList = this.getAggregationList();
        result = result * PRIME + ($aggregationList == null ? 43 : $aggregationList.hashCode());
        return result;
    }

    public String toString() {
        return "CollectionWithStats(collection=" + this.getCollection() + ", aggregationList=" + this.getAggregationList() + ")";
    }
}
