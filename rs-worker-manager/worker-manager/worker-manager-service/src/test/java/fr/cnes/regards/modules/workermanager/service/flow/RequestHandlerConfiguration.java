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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

public class RequestHandlerConfiguration {

    public final static String AVAILABLE_CONTENT_TYPE = "available_content";

    public final static String AVAILABLE_WORKER_TYPE = "workerType1";

    @Bean
    @Primary
    public WorkerCacheService workerCacheMock() {
        WorkerCacheService localMockWorkerCacheService = Mockito.mock(WorkerCacheService.class);
        Mockito.when(localMockWorkerCacheService.getWorkerTypeByContentType(anyString())).then(invocation -> {
            if (AVAILABLE_CONTENT_TYPE.equals(invocation.getArguments()[0]) ) {
                return Optional.of(AbstractWorkerManagerIT.DEFAULT_WORKER);
            }
            return Optional.empty();
        });
        return localMockWorkerCacheService;
    }

}
