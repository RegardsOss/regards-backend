package fr.cnes.regards.framework.modules.workspace.service.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.modules.workspace.service.DefaultWorkspaceNotifier;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceNotifier;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.modules.workspace.service.WorkspaceService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class WorkspaceAutoConf {

    @Bean
    @ConditionalOnMissingBean(IWorkspaceService.class)
    public IWorkspaceService workspaceService() {
        return new WorkspaceService();
    }

    @Bean
    @ConditionalOnMissingBean(IWorkspaceNotifier.class)
    public IWorkspaceNotifier workspaceNotifier() {
        return new DefaultWorkspaceNotifier();
    }

}
