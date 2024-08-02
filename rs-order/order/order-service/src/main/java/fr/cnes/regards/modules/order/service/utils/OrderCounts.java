/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.order.domain.OrderDataFile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Allows to count internal/external files and sub-orders during the order creation.
 * <p>
 * {@link #add(OrderCounts, OrderCounts)} allows to combine easily two instances.
 *
 * @author Guillaume Andrieu
 */
public final class OrderCounts {

    /**
     * Count of files managed by Storage (internal)
     */
    private int internalFilesCount;

    /**
     * Count of external files
     */
    private int externalFilesCount;

    /**
     * Count of internal subOrder created to compute expiration date
     */
    private int internalSubOrderCount;

    /**
     * Count of external subOrder created to compute expiration date
     */
    private int externalSubOrderCount;

    /**
     * Count of features concerned by the order
     */
    private int featuresCount;

    /**
     * Total size of files
     */
    private long totalFilesSize;

    private Set<UUID> jobInfoIdSet = new HashSet<>();

    public OrderCounts(int internalFilesCount,
                       int externalFilesCount,
                       int internalSubOrderCount,
                       int featuresCount,
                       long totalFilesSize) {
        this.internalFilesCount = internalFilesCount;
        this.externalFilesCount = externalFilesCount;
        this.internalSubOrderCount = internalSubOrderCount;
        this.featuresCount = featuresCount;
        this.totalFilesSize = totalFilesSize;
    }

    public OrderCounts(int internalFilesCount,
                       int externalFilesCount,
                       int internalSubOrderCount,
                       int externalSubOrderCount,
                       int featuresCount,
                       long totalFilesSize,
                       Set<UUID> jobInfoIdSet) {
        this.internalFilesCount = internalFilesCount;
        this.externalFilesCount = externalFilesCount;
        this.internalSubOrderCount = internalSubOrderCount;
        this.externalSubOrderCount = externalSubOrderCount;
        this.featuresCount = featuresCount;
        this.totalFilesSize = totalFilesSize;
        this.jobInfoIdSet = jobInfoIdSet;
    }

    public OrderCounts(int internalFilesCount,
                       int externalFilesCount,
                       int internalSubOrderCount,
                       Set<UUID> jobInfoIdSet) {
        this.internalFilesCount = internalFilesCount;
        this.externalFilesCount = externalFilesCount;
        this.internalSubOrderCount = internalSubOrderCount;
        this.jobInfoIdSet = jobInfoIdSet;
        this.totalFilesSize = 0l;
        this.featuresCount = 0;
    }

    public OrderCounts() {
    }

    public OrderCounts addToInternalFilesCount(int add) {
        this.internalFilesCount += add;
        return this;
    }

    public OrderCounts addToExternalFilesCount(int add) {
        this.externalFilesCount += add;
        return this;
    }

    public OrderCounts addToInternalSubOrderCount(int add) {
        this.internalSubOrderCount += add;
        return this;
    }

    public OrderCounts addToExternalSubOrderCount(int add) {
        this.externalSubOrderCount += add;
        return this;
    }

    public OrderCounts addJobInfoId(UUID jobInfoId) {
        jobInfoIdSet.add(jobInfoId);
        return this;
    }

    public OrderCounts addFeaturesCount(int add) {
        this.featuresCount += add;
        return this;
    }

    public OrderCounts addFileSize(long add) {
        this.totalFilesSize += add;
        return this;
    }

    public OrderCounts addTotalFileSizeOf(Set<OrderDataFile> dataFiles) {
        long sum = dataFiles.stream()
                            .map(OrderDataFile::getFilesize)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .sum();
        return addFileSize(sum);
    }

    public void incrInternalSubOrderCount() {
        this.addToInternalSubOrderCount(1);
    }

    public void incrExternalSubOrderCount() {
        this.addToExternalSubOrderCount(1);
    }

    public int getInternalFilesCount() {
        return internalFilesCount;
    }

    public int getExternalFilesCount() {
        return externalFilesCount;
    }

    public Long getTotalFilesSize() {
        return totalFilesSize;
    }

    public int getInternalSubOrderCount() {
        return internalSubOrderCount;
    }

    public int getExternalSubOrderCount() {
        return externalSubOrderCount;
    }

    public Set<UUID> getJobInfoIdSet() {
        return jobInfoIdSet;
    }

    public int getFeaturesCount() {
        return featuresCount;
    }

    public static OrderCounts initial() {
        return new OrderCounts();
    }

    public static OrderCounts add(OrderCounts one, OrderCounts two) {
        Set<UUID> mergedSet = new HashSet<>(one.getJobInfoIdSet());
        mergedSet.addAll(two.getJobInfoIdSet());
        return new OrderCounts(one.internalFilesCount + two.internalFilesCount,
                               one.externalFilesCount + two.externalFilesCount,
                               one.internalSubOrderCount + two.internalSubOrderCount,
                               one.externalSubOrderCount + two.externalSubOrderCount,
                               one.featuresCount + two.featuresCount,
                               one.totalFilesSize + two.totalFilesSize,
                               mergedSet);
    }
}
