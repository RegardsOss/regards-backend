package fr.cnes.regards.framework.modules.session.management.service;

import fr.cnes.regards.framework.modules.session.management.service.controllers.SessionService;
import fr.cnes.regards.framework.modules.session.management.service.controllers.SourceService;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotJobService;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotScheduler;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for session management service
 * @author Iliana Ghazali
 **/

@Configuration
public class SessionManagementServiceAutoConfiguration {

    /**
     * Update
     */

    @Bean
    public ManagerSnapshotJobService managerJobService() {
        return new ManagerSnapshotJobService();
    }

    @Bean
    public ManagerSnapshotService managerService() {
        return new ManagerSnapshotService();
    }

    @Bean
    public ManagerSnapshotScheduler managerScheduler() {
        return new ManagerSnapshotScheduler();
    }

    /**
     * Controller services
     */
    @Bean
    public SourceService sourceService() {
        return new SourceService();
    }

    @Bean
    public SessionService sessionService() {
        return new SessionService();
    }
}