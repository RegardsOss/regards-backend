package fr.cnes.regards.modules.workermanager.service.cache;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import fr.cnes.regards.modules.workermanager.domain.cache.CacheEntry;
import fr.cnes.regards.modules.workermanager.domain.cache.CacheWorkerInstance;
import fr.cnes.regards.modules.workermanager.dto.WorkerTypeAlive;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;

@Service
public class WorkerCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerCacheService.class);

    private static final String DEFAULT_EXPIRE_IN_CACHE_DURATION = "15";

    @Value("${regards.workermanager.cache.expiration.heartbeat:" + DEFAULT_EXPIRE_IN_CACHE_DURATION + "}")
    public long expireInCacheDuration;

    /**
     * Cache to save worker heartbeats, the key is the workerType and the value {@link CacheEntry} contains
     * the workerInstance list related to this workerType
     * This cache invalid automatically old workerType (that did not receive heartbeat since {@link WorkerCacheService#expireInCacheDuration} sec)
     * And the CacheEntry removes old heartbeat when method {@link CacheEntry#addWorkers(Set requests)} called
     */
    private Cache<String, CacheEntry> cache;

    @Autowired
    private WorkerConfigCacheService workerConfigCacheService;

    @PostConstruct
    private void initCache() {
        cache = CacheBuilder.newBuilder().expireAfterWrite(Duration.of(expireInCacheDuration, ChronoUnit.SECONDS))
                .build();
    }

    public Cache<String, CacheEntry> getCache() {
        return cache;
    }

    /**
     * Update the cache using heart beats
     *
     * @param events a list of heart beats messages
     */
    public void registerWorkers(List<WorkerHeartBeatEvent> events) {
        // Regroup events by worker type, to avoid multiple cache write
        Map<String, Set<CacheWorkerInstance>> workerInsSetByWorkerType = getWorkerInsSetByWorkerType(events);
        // Update the cache
        updateCache(workerInsSetByWorkerType);
    }

    /**
     * Update the cache
     *
     * @param workerInsSetByWorkerType
     */
    private void updateCache(Map<String, Set<CacheWorkerInstance>> workerInsSetByWorkerType) {
        // Iterate over worker types from requests received
        for (String workerType : workerInsSetByWorkerType.keySet()) {
            Set<CacheWorkerInstance> workerInsSet = workerInsSetByWorkerType.get(workerType);
            // check if an entry already exists in the cache
            CacheEntry cacheEntry = cache.getIfPresent(workerType);
            if (cacheEntry != null) {
                cacheEntry.addWorkers(workerInsSet);
            } else {
                cache.put(workerType, new CacheEntry(workerInsSet, expireInCacheDuration));
            }
            LOGGER.debug("{} heartbeat(s) received from worker type {}", workerInsSet.size(), workerType);
        }
    }

    /**
     * @param contentType Content Type of a request
     * @return an optional containing the workerType when the cache contains a worker
     *              accepting provided content type and tenant,
     *              empty otherwise
     */
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

    public List<WorkerTypeAlive> getWorkersInstance(List<String> contentTypes) {
        List<WorkerTypeAlive> result = new ArrayList<>();
        Set<String> workerTypesToKeep = new HashSet<>();
        Map<String, String> workerConfigs = workerConfigCacheService.getWorkerConfigs();
        // Init worker types to keep
        for(Map.Entry <String, String> workerConfig : workerConfigs.entrySet()) {
            String workerType = workerConfig.getValue();
            if (contentTypes == null || contentTypes.isEmpty()) {
                // Don't ignore any worker type
                workerTypesToKeep.add(workerType);
            } else {
                // Ignore all worker types not having their content types referenced in provided contentTypes
                String contentType = workerConfig.getKey();
                if (contentTypes.contains(contentType)) {
                    workerTypesToKeep.add(workerType);
                }
            }
        }
        // Compute result
        for (Map.Entry<String, CacheEntry> entry: cache.asMap().entrySet()) {
            String workerType = entry.getKey();
            if (workerTypesToKeep.contains(workerType)) {
                result.add(new WorkerTypeAlive(workerType, entry.getValue().getNbWorkerIns()));
            }
        }
        return result;
    }

    /**
     * Regroup events by worker type and transform events into {@link CacheWorkerInstance}
     */
    private Map<String, Set<CacheWorkerInstance>> getWorkerInsSetByWorkerType(List<WorkerHeartBeatEvent> events) {
        Map<String, Set<CacheWorkerInstance>> messagesByWorkerType = new HashMap<>();
        events.stream()
                // Remove events outdated
                .filter(event -> CacheEntry.isValidHeartBeat(event.getHeartBeatDate(), expireInCacheDuration))
                // Regroup events into a Map with workerType as key and a list of CacheWorkerIns as value
                .forEach(workerHeartBeatEvent -> {
                    String workerType = workerHeartBeatEvent.getType();
                    // Convert the event into a CacheWorkerIns
                    CacheWorkerInstance cacheWorkerInstance = CacheWorkerInstance.build(workerHeartBeatEvent);
                    // Save it to the returned Map
                    if (messagesByWorkerType.containsKey(workerType)) {
                        messagesByWorkerType.get(workerType).add(cacheWorkerInstance);
                    } else {
                        messagesByWorkerType.put(workerType, Sets.newHashSet(cacheWorkerInstance));
                    }
                });
        return messagesByWorkerType;
    }
}
