package fr.cnes.regards.framework.modules.workspace.service.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.modules.workspace.service.DefaultWorkspaceNotifier;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceNotifier;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.modules.workspace.service.WorkspaceService;

/**
 * Workspace module auto configuration
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class WorkspaceAutoConf {

    /**
     * @return workspace default service implementation
     */
    @Bean
    @ConditionalOnMissingBean(IWorkspaceService.class)
    public IWorkspaceService workspaceService() {
        return new WorkspaceService();
    }

    /**
     * @return workspace default notifier implementation
     */
    @Bean
    @ConditionalOnMissingBean(IWorkspaceNotifier.class)
    public IWorkspaceNotifier workspaceNotifier() {
        return new DefaultWorkspaceNotifier();
    }

}
