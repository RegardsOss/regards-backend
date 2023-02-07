package fr.cnes.regards.modules.workermanager.service.cache;/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Scheduler to remove worker types with all instances expired.
 * Check for lastUpdateDate of all CacheEntry.
 *
 * @author Sébastien Binda
 **/

@Component
public class CacheEvictScheduler {

    private static final String DEFAULT_INITIAL_DELAY = "5000";

    private static final String DEFAULT_SCHEDULING_DELAY = "2000";

    @Autowired
    private WorkerCacheService workerCacheService;

    @Scheduled(initialDelayString = "${regards.workers.cache.evict.scheduling.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.workers.cache.evict.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void evict() {
        OffsetDateTime now = OffsetDateTime.now();
        workerCacheService.getCache().asMap().forEach((workerType, entry) -> {
            if (entry.getLastUpdateDate().plusSeconds(workerCacheService.getExpireInCacheDuration()).isBefore(now)) {
                workerCacheService.getCache().invalidate(workerType);
            }
        });
    }

}
