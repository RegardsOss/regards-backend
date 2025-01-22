/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.client;

import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.file.packager.service.scheduler.CompletePackageScheduler;
import fr.cnes.regards.modules.file.packager.service.scheduler.RetryFilePackagingScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for {@link FilePackagerClientIT} that exceptionally requires the scheduler to be defined.
 *
 * @author Thibaud Michaudel
 **/
@TestConfiguration
public class FilePackagerClientTestConfig {

    @Autowired
    LockingTaskExecutors lockingTaskExecutors;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    ITenantResolver tenantResolver;

    @Autowired
    FilePackagerService filePackagerService;

    @Bean
    public CompletePackageScheduler completePackageScheduler() {
        return new CompletePackageScheduler(lockingTaskExecutors,
                                            runtimeTenantResolver,
                                            tenantResolver,
                                            filePackagerService);
    }

    @Bean
    public RetryFilePackagingScheduler retryFilePackagingScheduler() {
        return new RetryFilePackagingScheduler(lockingTaskExecutors,
                                               runtimeTenantResolver,
                                               tenantResolver,
                                               filePackagerService);
    }

    @Bean
    public CompletePackageScheduler completePackageSchedulerWithRetry() {
        return new CompletePackageScheduler(lockingTaskExecutors,
                                            runtimeTenantResolver,
                                            tenantResolver,
                                            filePackagerService);
    }
}
