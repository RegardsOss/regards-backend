/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.ConsensusBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.endpoint.ResourceAccessVoter;
import fr.cnes.regards.framework.security.endpoint.RootResourceAccessVoter;

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
@ConditionalOnWebApplication

@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
public class MethodSecurityAutoConfiguration extends GlobalMethodSecurityConfiguration {

    /**
     * Global method authorization service
     */
    @Autowired
    private MethodAuthorizationService methodAuthService;

    @Override
    protected AccessDecisionManager accessDecisionManager() {
        final List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();
        decisionVoters.add(new ResourceAccessVoter(methodAuthService));
        decisionVoters.add(new RootResourceAccessVoter());
        // Access granted if one of the two voter return access granted
        final ConsensusBased decision = new ConsensusBased(decisionVoters);
        decision.setAllowIfEqualGrantedDeniedDecisions(true);
        return decision;
    }
}
