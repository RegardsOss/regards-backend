package fr.cnes.regards.framework.modules.session.management.service;

import fr.cnes.regards.framework.modules.session.commons.service.SessionCommonsServiceAutoConfiguration;
import fr.cnes.regards.framework.modules.session.management.service.clean.session.ManagerCleanJobService;
import fr.cnes.regards.framework.modules.session.management.service.clean.session.ManagerCleanScheduler;
import fr.cnes.regards.framework.modules.session.management.service.clean.session.ManagerCleanService;
import fr.cnes.regards.framework.modules.session.management.service.clean.snapshotprocess.ManagerCleanSnapshotProcessJobService;
import fr.cnes.regards.framework.modules.session.management.service.clean.snapshotprocess.ManagerCleanSnapshotProcessScheduler;
import fr.cnes.regards.framework.modules.session.management.service.clean.snapshotprocess.ManagerCleanSnapshotProcessService;
import fr.cnes.regards.framework.modules.session.management.service.controllers.SessionManagerService;
import fr.cnes.regards.framework.modules.session.management.service.controllers.SourceManagerService;
import fr.cnes.regards.framework.modules.session.management.service.handlers.SessionManagerHandler;
import fr.cnes.regards.framework.modules.session.management.service.handlers.SessionManagerHandlerService;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotJobService;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotScheduler;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotService;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for session management service
 *
 * @author Iliana Ghazali
 **/

@Configuration
@AutoConfigureBefore({ SessionCommonsServiceAutoConfiguration.class })
public class SessionManagementServiceAutoConfiguration {

    /**
     * Update
     */
    @Bean
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