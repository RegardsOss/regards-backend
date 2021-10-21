package fr.cnes.regards.modules.workermanager.service.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import fr.cnes.regards.modules.workermanager.domain.cache.CacheEntry;
import fr.cnes.regards.modules.workermanager.domain.cache.CacheWorkerIns;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class WorkerCacheService implements IWorkerCacheService {

    private static final String DEFAULT_EXPIRE_IN_CACHE_DURATION = "15";

    @Value("${regards.workermanager.cache.expiration.heartbeat:" + DEFAULT_EXPIRE_IN_CACHE_DURATION + "}")
    public long EXPIRE_IN_CACHE_DURATION;

    /**
     * Cache to save worker heartbeats, the key is the workerType and the value {@link CacheEntry} contains
     * the workerInstance list related to this workerType
     * This cache invalid automatically old workerType (that did not receive heartbeat since {@link WorkerCacheService#EXPIRE_IN_CACHE_DURATION} sec)
     * And the CacheEntry removes old heartbeat when method {@link CacheEntry#addWorkers(Set requests)} called
     */
    private Cache<String, CacheEntry> cache;

    @Autowired
    private WorkerConfigCacheService workerConfigCacheService;

    @PostConstruct
    private void initCache() {
        cache = CacheBuilder.newBuilder().expireAfterWrite(Duration.of(EXPIRE_IN_CACHE_DURATION, ChronoUnit.SECONDS))
                .build();
    }

    @Override
    public Cache<String, CacheEntry> getCache() {
        return cache;
    }

    @Override
    public void registerWorkers(List<WorkerHeartBeatEvent> events) {
        // Regroup events by worker type, to avoid multiple cache write
        Map<String, Set<CacheWorkerIns>> workerInsSetByWorkerType = getWorkerInsSetByWorkerType(events);
        // Update the cache
        updateCache(workerInsSetByWorkerType);
    }

    /**
     * Update the cache
     *
     * @param workerInsSetByWorkerType
     */
    private void updateCache(Map<String, Set<CacheWorkerIns>> workerInsSetByWorkerType) {
        // Iterate over worker types from requests received
        for (String workerType : workerInsSetByWorkerType.keySet()) {
            Set<CacheWorkerIns> workerInsSet = workerInsSetByWorkerType.get(workerType);
            // check if an entry already exists in the cache
            CacheEntry cacheEntry = cache.getIfPresent(workerType);
            if (cacheEntry != null) {
                cacheEntry.addWorkers(workerInsSet);
            } else {
                cache.put(workerType, new CacheEntry(workerInsSet, EXPIRE_IN_CACHE_DURATION));
            }
        }
    }

    @Override
    public Optional<String> getWorkerTypeByContentType(String contentType) {
        Optional<String> workerTypeOpt = workerConfigCacheService.getWorkerType(contentType);
        if (workerTypeOpt.isPresent()) {
            CacheEntry cacheEntry = cache.getIfPresent(workerTypeOpt.get());
            // Check if the cache contains a living worker
            if (cacheEntry != null) {
                return workerTypeOpt;
            }
        }
        return Optional.empty();
    }

    /**
     * Regroup events by worker type and transform events into {@link CacheWorkerIns}
     */
    private Map<String, Set<CacheWorkerIns>> getWorkerInsSetByWorkerType(List<WorkerHeartBeatEvent> events) {
        Map<String, Set<CacheWorkerIns>> messagesByWorkerType = new HashMap<>();
        events.stream()
                // Remove events outdated
                .filter(event -> CacheEntry.isValidHeartBeat(event.getHeartBeatDate(), EXPIRE_IN_CACHE_DURATION))
                // Regroup events into a Map with workerType as key and a list of CacheWorkerIns as value
                .forEach(workerHeartBeatEvent -> {
                    String workerType = workerHeartBeatEvent.getType();
                    // Convert the event into a CacheWorkerIns
                    CacheWorkerIns cacheWorkerIns = CacheWorkerIns.build(workerHeartBeatEvent);
                    // Save it to the returned Map
                    if (messagesByWorkerType.containsKey(workerType)) {
                        messagesByWorkerType.get(workerType).add(cacheWorkerIns);
                    } else {
                        messagesByWorkerType.put(workerType, Sets.newHashSet(cacheWorkerIns));
                    }
                });
        return messagesByWorkerType;
    }
}
