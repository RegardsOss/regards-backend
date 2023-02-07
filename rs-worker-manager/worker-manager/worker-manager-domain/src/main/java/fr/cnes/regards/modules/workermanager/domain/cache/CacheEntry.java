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
package fr.cnes.regards.modules.workermanager.domain.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * An entry used by the cache, holds all references to workers {@link CacheWorkerInstance} handling a specific content type
 *
 * @author Léo Mieulet
 */
public class CacheEntry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheEntry.class);

    /**
     * The duration before assuming the last heartbeat of a workerIns is useless to store here
     */
    private final long expireInCacheDuration;

    /**
     * Set of Worker instances matching the cache key
     */
    Set<CacheWorkerInstance> workerInsList;

    private OffsetDateTime lastUpdateDate;

    public CacheEntry(Set<CacheWorkerInstance> workerInsList, long expireInCacheDuration) {
        this.workerInsList = workerInsList;
        this.expireInCacheDuration = expireInCacheDuration;
        this.lastUpdateDate = OffsetDateTime.now();
        this.removeOutdatedWorkerIns();
    }

    public void addWorkers(Set<CacheWorkerInstance> workerInsList) {
        workerInsList.forEach(newInstance -> {
            Optional<CacheWorkerInstance> inst = findInstanceById(newInstance.getId());
            if (inst.isPresent()) {
                inst.get().setLastHeartBeatDate(newInstance.getLastHeartBeatDate());
            } else {
                LOGGER.info("New instance for worker {} registered", newInstance.getWorkerType());
                this.workerInsList.add(newInstance);
            }
        });
        this.lastUpdateDate = OffsetDateTime.now();
        this.removeOutdatedWorkerIns();
    }

    private Optional<CacheWorkerInstance> findInstanceById(String id) {
        return this.workerInsList.stream().filter(w -> w.getId().equals(id)).findFirst();
    }

    /**
     * Iterate over the list of WorkerIns and remove instances that are outdated
     */
    private void removeOutdatedWorkerIns() {
        this.workerInsList.removeIf(cacheWorkerInstance -> {
            boolean valid = isValidHeartBeat(cacheWorkerInstance.getLastHeartBeatDate(), expireInCacheDuration, false);
            if (!valid) {
                LOGGER.info("Instance {} for {} worker expired",
                            cacheWorkerInstance.getId(),
                            cacheWorkerInstance.getWorkerType());
            }
            return !valid;
        });
    }

    /**
     * Function used to check if an heartbeat is considered valid
     */
    public static boolean isValidHeartBeat(OffsetDateTime lastHeartBeatDate,
                                           long expireInCacheDuration,
                                           boolean enableLog) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean valid = lastHeartBeatDate.plusSeconds(expireInCacheDuration).isAfter(now);
        if (!valid) {
            LOGGER.warn("Invalid heartbeat from {} received at {}", lastHeartBeatDate, now);
        }
        return valid;
    }

    public Long getNbWorkerIns() {
        return Long.valueOf(workerInsList.size());
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }
}
