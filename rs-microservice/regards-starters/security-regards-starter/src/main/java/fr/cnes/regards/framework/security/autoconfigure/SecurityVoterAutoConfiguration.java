/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.endpoint.voter.InstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.ProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.ResourceAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.SystemAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.ISystemAccessVoter;

/**
 * This class autoconfigures required voters based on configuration.
 *
 * @author Marc Sordi
 *
 */
public class SecurityVoterAutoConfiguration {

    /**
     * Global method authorization service
     */
    @Autowired
    private MethodAuthorizationService methodAuthService;

    /**
     * Give full access for internal system call between microservices
     *
     * @return {@link ISystemAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "system.voter.enabled", havingValue = "true")
    @Bean
    public ISystemAccessVoter systemAccessVoter() {
        return new SystemAccessVoter();
    }

    /**
     * Give full access for instance admin call
     *
     * @return {@link IInstanceAdminAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "instance.voter.enabled", havingValue = "true")
    @Bean
    public IInstanceAdminAccessVoter instanceAccessVoter() {
        return new InstanceAdminAccessVoter();
    }

    /**
     * Give full access for project admin call
     *
     * @return {@link IInstanceAdminAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "project.admin.voter.enabled", havingValue = "true")
    @Bean
    public IProjectAdminAccessVoter adminAccessVoter() {
        return new ProjectAdminAccessVoter();
    }

    /**
     * Manage dynamic endpoint security based on roles
     *
     * @return {@link ResourceAccessVoter}
     */
    @ConditionalOnMissingBean
    @Bean
    public ResourceAccessVoter resourceAccessVoter() {
        return new ResourceAccessVoter(methodAuthService);
    }
}
