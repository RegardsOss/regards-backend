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
package fr.cnes.regards.modules.workermanager.domain.cache;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * An entry used by the cache, holds all references to workers {@link CacheWorkerInstance} handling a specific content type
 *
 * @author Léo Mieulet
 */
public class CacheEntry {

    /**
     * The duration before assuming the last heartbeat of a workerIns is useless to store here
     */
    private final long expireInCacheDuration;

    /**
     * Set of Worker instances matching the cache key
     */
    Set<CacheWorkerInstance> workerInsList;

    public CacheEntry(Set<CacheWorkerInstance> workerInsList, long expireInCacheDuration) {
        this.workerInsList = workerInsList;
        this.expireInCacheDuration = expireInCacheDuration;
        this.removeOutdatedWorkerIns();
    }

    public void addWorkers(Set<CacheWorkerInstance> workerInsList) {
        this.workerInsList.removeAll(workerInsList);
        this.workerInsList.addAll(workerInsList);
        this.removeOutdatedWorkerIns();
    }

    /**
     * Iterate over the list of WorkerIns and remove instances that are outdated
     */
    private void removeOutdatedWorkerIns() {
        this.workerInsList.removeIf(
                cacheWorkerInstance -> !isValidHeartBeat(cacheWorkerInstance.getLastHeartBeatDate(), expireInCacheDuration));
    }

    /**
     * Function used to check if an heartbeat is considered valid
     * @param lastHeartBeatDate
     * @param expireInCacheDuration
     * @return
     */
    public static boolean isValidHeartBeat(OffsetDateTime lastHeartBeatDate, long expireInCacheDuration) {
        return lastHeartBeatDate.plusSeconds(expireInCacheDuration)
                .isAfter(OffsetDateTime.now());
    }

}
