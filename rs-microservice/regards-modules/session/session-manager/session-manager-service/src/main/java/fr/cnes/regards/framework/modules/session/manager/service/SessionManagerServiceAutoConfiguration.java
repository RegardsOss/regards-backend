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
package fr.cnes.regards.framework.modules.session.manager.service;

import fr.cnes.regards.framework.modules.session.commons.service.SessionCommonsServiceAutoConfiguration;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanJobService;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanScheduler;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanService;
import fr.cnes.regards.framework.modules.session.manager.service.clean.snapshotprocess.ManagerCleanSnapshotProcessJobService;
import fr.cnes.regards.framework.modules.session.manager.service.clean.snapshotprocess.ManagerCleanSnapshotProcessScheduler;
import fr.cnes.regards.framework.modules.session.manager.service.clean.snapshotprocess.ManagerCleanSnapshotProcessService;
import fr.cnes.regards.framework.modules.session.manager.service.controllers.SessionManagerService;
import fr.cnes.regards.framework.modules.session.manager.service.controllers.SourceManagerService;
import fr.cnes.regards.framework.modules.session.manager.service.handlers.SessionManagerHandler;
import fr.cnes.regards.framework.modules.session.manager.service.handlers.SessionManagerHandlerService;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotJobService;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotScheduler;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotService;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Autoconfiguration for session management service
 *
 * @author Iliana Ghazali
 **/

@Configuration
@AutoConfigureBefore({ SessionCommonsServiceAutoConfiguration.class })
public class SessionManagerServiceAutoConfiguration {

    /**
     * Update
     */
    @Bean
    @Profile("!noscheduler")
    public ManagerSnapshotScheduler managerScheduler() {
        return new ManagerSnapshotScheduler();
    }

    @Bean
    public ManagerSnapshotJobService managerJobService() {
        return new ManagerSnapshotJobService();
    }

    @Bean
    public ManagerSnapshotService managerService() {
        return new ManagerSnapshotService();
    }

    /**
     * Clean
     */

    @Bean
    @Profile("!noscheduler")
    public ManagerCleanScheduler managerCleanScheduler() {
        return new ManagerCleanScheduler();
    }

    @Bean
    public ManagerCleanJobService managerCleanJobService() {
        return new ManagerCleanJobService();
    }

    @Bean
    public ManagerCleanService managerCleanService() {
        return new ManagerCleanService();
    }

    @Bean
    @Profile("!noscheduler")
    public ManagerCleanSnapshotProcessScheduler managerCleanSnapshotProcessScheduler() {
        return new ManagerCleanSnapshotProcessScheduler();
    }

    @Bean
    public ManagerCleanSnapshotProcessJobService managerCleanSnapshotProcessJobService() {
        return new ManagerCleanSnapshotProcessJobService();
    }

    @Bean
    public ManagerCleanSnapshotProcessService managerCleanSnapshotProcessService() {
        return new ManagerCleanSnapshotProcessService();
    }

    /**
     * Handler
     */
    @Bean
    @Profile("!nohandler")
    public SessionManagerHandler managerHandler() {
        return new SessionManagerHandler();
    }

    @Bean
    public SessionManagerHandlerService sessionManagerHandlerService() {
        return new SessionManagerHandlerService();
    }

    /**
     * Controller services
     */
    @Bean
    public SourceManagerService sourceManagerService() {
        return new SourceManagerService();
    }

    @Bean
    public SessionManagerService sessionManagerService() {
        return new SessionManagerService();
    }
}