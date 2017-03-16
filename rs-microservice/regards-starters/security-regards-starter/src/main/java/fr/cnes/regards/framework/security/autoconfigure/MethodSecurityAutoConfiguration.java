/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import fr.cnes.regards.framework.security.endpoint.voter.ResourceAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.ISystemAccessVoter;

/**
 * This class allow to add a security filter on method access. Each time a method is called, the accessDecisionManager
 * check if the connected user can access the method via the ResourceAccessVoter class.
 *
 * {@link EnableGlobalMethodSecurity#proxyTargetClass()} is required to manage controller interface in SPRING MVC
 * controllers.
 *
 * @author CS SI
 *
 */
@Configuration
@ConditionalOnWebApplication
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
public class MethodSecurityAutoConfiguration extends GlobalMethodSecurityConfiguration {

    @Autowired
    private ResourceAccessVoter resourceAccessVoter;

    @Autowired(required = false)
    private IInstanceAdminAccessVoter instanceAccessVoter;

    @Autowired(required = false)
    private ISystemAccessVoter systemAccessVoter;

    @Autowired(required = false)
    private IProjectAdminAccessVoter adminAccessVoter;

    @Bean
    @Override
    protected AccessDecisionManager accessDecisionManager() {
        final List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();

        // Manage system voter
        if (systemAccessVoter != null) {
            decisionVoters.add(systemAccessVoter);
        }

        // Manage instance voter
        if (instanceAccessVoter != null) {
            decisionVoters.add(instanceAccessVoter);
        }

        // Manage project admin voter
        if (adminAccessVoter != null) {
            decisionVoters.add(adminAccessVoter);
        }

        decisionVoters.add(resourceAccessVoter);

        // Access granted if one of the two voter return access granted
        return new AffirmativeBased(decisionVoters);
    }
}
