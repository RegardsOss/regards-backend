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
package fr.cnes.regards.modules.order.service.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Allows to count internal/external files and suborders during the order creation.
 * <p>
 * {@link #add(OrderCounts, OrderCounts)} allows to combine easily two instances.
 *
 * @author Guillaume Andrieu
 */
public final class OrderCounts {

    // Count of files managed by Storage (internal)
    private int internalFilesCount;

    // External files count
    private int externalFilesCount;

    // Count number of subOrder created to compute expiration date
    private int subOrderCount;

    private Set<UUID> jobInfoIdSet = new HashSet<>();

    public OrderCounts(int internalFilesCount, int externalFilesCount, int subOrderCount) {
        this.internalFilesCount = internalFilesCount;
        this.externalFilesCount = externalFilesCount;
        this.subOrderCount = subOrderCount;
    }

    public OrderCounts(int internalFilesCount, int externalFilesCount, int subOrderCount, Set<UUID> jobInfoIdSet) {
        this.internalFilesCount = internalFilesCount;
        this.externalFilesCount = externalFilesCount;
        this.subOrderCount = subOrderCount;
        this.jobInfoIdSet = jobInfoIdSet;
    }

    public OrderCounts() {
        this(0, 0, 0);
    }

    public OrderCounts addToInternalFilesCount(int add) {
        this.internalFilesCount += add;
        return this;
    }

    public OrderCounts addToExternalFilesCount(int add) {
        this.externalFilesCount += add;
        return this;
    }

    public OrderCounts addToSubOrderCount(int add) {
        this.subOrderCount += add;
        return this;
    }

    public OrderCounts addJobInfoId(UUID jobInfoId) {
        jobInfoIdSet.add(jobInfoId);
        return this;
    }

    public void incrSubOrderCount() {
        this.addToSubOrderCount(1);
    }

    public int getInternalFilesCount() {
        return internalFilesCount;
    }

    public int getExternalFilesCount() {
        return externalFilesCount;
    }

    public int getSubOrderCount() {
        return subOrderCount;
    }

    public Set<UUID> getJobInfoIdSet() {
        return jobInfoIdSet;
    }

    public static OrderCounts initial() {
        return new OrderCounts();
    }

    public static OrderCounts add(OrderCounts one, OrderCounts two) {
        Set<UUID> mergedSet = new HashSet<>(one.getJobInfoIdSet());
        mergedSet.addAll(two.getJobInfoIdSet());
        return new OrderCounts(one.internalFilesCount + two.internalFilesCount,
                               one.externalFilesCount + two.externalFilesCount,
                               one.subOrderCount + two.subOrderCount,
                               mergedSet);
    }

}
