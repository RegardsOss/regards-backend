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
package fr.cnes.regards.modules.workermanager.service.flow;

import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

public class RequestHandlerConfiguration {

    public final static String AVAILABLE_CONTENT_TYPE_0 = "available_content";

    public final static String AVAILABLE_CONTENT_TYPE_1 = "content1";

    public final static String AVAILABLE_CONTENT_TYPE_2 = "content2";

    public final static String AVAILABLE_CONTENT_TYPE_3 = "content3";

    private final static List<String> AVAILABLE_CONTENT_TYPES = List.of(AVAILABLE_CONTENT_TYPE_0,
                                                                        AVAILABLE_CONTENT_TYPE_1,
                                                                        AVAILABLE_CONTENT_TYPE_2,
                                                                        AVAILABLE_CONTENT_TYPE_3);


    public final static String AVAILABLE_WORKER_TYPE_1 = "workerType1";

    public final static String AVAILABLE_WORKER_TYPE_2 = "workerType2";

    private final static List<String> AVAILABLE_WORKER_TYPES = List.of(AbstractWorkerManagerIT.DEFAULT_WORKER,
                                                                       AVAILABLE_WORKER_TYPE_1,
                                                                       AVAILABLE_WORKER_TYPE_2);


    @Bean
    @Primary
    public WorkerCacheService createWorkerCacheServiceMock() {
        WorkerCacheService localMockWorkerCacheService = Mockito.mock(WorkerCacheService.class);
        // Mock getWorkerTypeByContentType
        Mockito.when(localMockWorkerCacheService.getWorkerTypeByContentType(anyString())).then(invocation -> {
            String contentType = (String) invocation.getArguments()[0];
            if (AVAILABLE_CONTENT_TYPES.contains(contentType)) {
                return Optional.of(AbstractWorkerManagerIT.DEFAULT_WORKER);
            }
            return Optional.empty();
        });
        // Mock isWorkerTypeInCache
        Mockito.when(localMockWorkerCacheService.isWorkerTypeInCache(anyString())).then(invocation -> {
            String workerType = (String) invocation.getArguments()[0];
            return AVAILABLE_WORKER_TYPES.contains(workerType);
        });
        return localMockWorkerCacheService;
    }

}
