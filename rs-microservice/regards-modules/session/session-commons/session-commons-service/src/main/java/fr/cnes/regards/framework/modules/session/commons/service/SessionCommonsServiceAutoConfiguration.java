package fr.cnes.regards.framework.modules.session.commons.service;

import fr.cnes.regards.framework.modules.session.commons.service.delete.DefaultSessionDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.DefaultSourceDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISessionDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISourceDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SessionDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SourceDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.jobs.SnapshotJobEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.jobs.SnapshotJobEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Iliana Ghazali
 **/

@Configuration
public class SessionCommonsServiceAutoConfiguration {

    /**
     * Delete
     */

    @Bean
    public SessionDeleteEventHandler sessionDeleteEventHandler() {
        return new SessionDeleteEventHandler();
    }

    @Bean
    public SourceDeleteEventHandler sourceDeleteEventHandler() {
        return new SourceDeleteEventHandler();
    }

    @Bean
    @ConditionalOnMissingBean(ISessionDeleteService.class)
    public ISessionDeleteService sessionDeleteService() {
        return new DefaultSessionDeleteService();
    }

    @Bean
    @ConditionalOnMissingBean(ISourceDeleteService.class)
    public ISourceDeleteService sourceDeleteService() {
        return new DefaultSourceDeleteService();
    }

    /**
     * Jobs
     */

    @Bean
    public SnapshotJobEventHandler snapshotJobEventHandler() {
        return new SnapshotJobEventHandler();
    }

    @Bean
    public SnapshotJobEventService snapshotJobEventService() {
        return new SnapshotJobEventService();
    }
}