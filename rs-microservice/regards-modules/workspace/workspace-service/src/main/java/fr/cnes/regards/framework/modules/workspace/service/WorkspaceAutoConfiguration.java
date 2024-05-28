package fr.cnes.regards.framework.modules.workspace.service;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Workspace module auto configuration
 * @author Sylvain VISSIERE-GUERINET
 */
@AutoConfiguration
public class WorkspaceAutoConfiguration {
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
