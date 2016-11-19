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
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.endpoint.ResourceAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IRoleSysAccessVoter;

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

    /**
     * Access voter for internal SYS roles.
     */
    @Autowired(required = false)
    private IRoleSysAccessVoter roleSysAccessVoter;

    /**
     * Access voter for specific instance admin user
     */
    @Autowired(required = false)
    private IInstanceAdminAccessVoter instanceAdminAccessVoter;

    @Override
    protected AccessDecisionManager accessDecisionManager() {
        final List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();

        if (roleSysAccessVoter != null) {
            decisionVoters.add(roleSysAccessVoter);
        }
        if (instanceAdminAccessVoter != null) {
            decisionVoters.add(instanceAdminAccessVoter);
        }

        decisionVoters.add(new ResourceAccessVoter(methodAuthService));

        // Access granted if one of the two voter return access granted
        final AffirmativeBased decision = new AffirmativeBased(decisionVoters);
        // decision.setAllowIfEqualGrantedDeniedDecisions(true);
        return decision;
    }
}
