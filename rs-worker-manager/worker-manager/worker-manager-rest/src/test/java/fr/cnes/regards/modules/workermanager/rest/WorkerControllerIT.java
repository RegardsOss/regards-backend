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
package fr.cnes.regards.modules.workermanager.rest;

import java.time.OffsetDateTime;
import java.util.*;

import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.RequestBuilder;

import org.assertj.core.util.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import fr.cnes.regards.modules.workermanager.service.config.ConfigManager;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigService;

/**
 * @author Théo Lasserre
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_controller_it" })
public class WorkerControllerIT extends AbstractRegardsIT {

    private final String workerType1 = "workerType1";

    private final String workerType2 = "workerType2";

    private final String workerType3 = "workerType3";

    private final Set<String> contentTypes1 = Sets.newHashSet("contentTypes1-1", "contentTypes1-2");

    private final Set<String> contentTypes2 = Sets.newHashSet("contentTypes2-1");

    private final Set<String> contentTypes3 = Sets.newHashSet("contentTypes3", "contentTypes3-2");

    @Autowired
    private WorkerCacheService workerCacheService;

    @Autowired
    private WorkerConfigService workerConfigService;

    @Autowired
    private WorkerConfigCacheService workerConfigCacheService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @MockBean
    private IAmqpAdmin amqpAdmin;

    @Test
    public void retrieveWorkerList() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Save configuration used by this test
        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(workerType1, contentTypes1),
                                                                new WorkerConfigDto(workerType2, contentTypes2),
                                                                new WorkerConfigDto(workerType3, contentTypes3)));

        String workerId1 = UUID.randomUUID().toString();
        String workerId2 = UUID.randomUUID().toString();
        String workerId3 = UUID.randomUUID().toString();

        workerCacheService.registerWorkers(
                Lists.list(new WorkerHeartBeatEvent(workerId1, workerType1, OffsetDateTime.now()),
                                                 new WorkerHeartBeatEvent(workerId2, workerType2, OffsetDateTime.now()),
                                                 new WorkerHeartBeatEvent(workerId3, workerType3, OffsetDateTime.now())
                ));

        Assert.assertEquals("Invalid number of element in cache",3L,workerCacheService.getCache().asMap().keySet().size());

        // Without contentTypes parameters
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatusOk();
        requestBuilderCustomizer.expectToHaveSize(JSON_PATH_STAR, 3);
        performDefaultGet(WorkerController.TYPE_MAPPING, requestBuilderCustomizer, "Error retrieving workers without contentTypes parameter");
        // With contentTypes parameters
        RequestBuilderCustomizer requestBuilderCustomizer2 = customizer();
        requestBuilderCustomizer2.expectStatusOk();
        requestBuilderCustomizer2.addParameter("contentTypes", "contentTypes1-1", "contentTypes1-2", "contentTypes2-1");
        requestBuilderCustomizer2.expectToHaveSize(JSON_PATH_STAR, 2);
        performDefaultGet(WorkerController.TYPE_MAPPING, requestBuilderCustomizer2, "Error retrieving workers with contentTypes parameter");
    }
}
