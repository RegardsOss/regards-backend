package fr.cnes.regards.modules.workermanager.service.cache;

import com.google.common.cache.Cache;
import fr.cnes.regards.modules.workermanager.domain.cache.CacheEntry;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;

import java.util.List;
import java.util.Optional;

/**
 * @author Léo Mieulet
 */
public interface IWorkerCacheService {

    Cache<String, CacheEntry> getCache();

    /**
     * Update the cache using heart beats
     *
     * @param messages a list of heart beats messages
     */
    void registerWorkers(List<WorkerHeartBeatEvent> messages);

    /**
     * @param contentType Content Type of a request
     * @return an optional containing the workerType when the cache contains a worker
     *              accepting provided content type and tenant,
     *              empty otherwise
     */
    Optional<String> getWorkerTypeByContentType(String contentType);
}
