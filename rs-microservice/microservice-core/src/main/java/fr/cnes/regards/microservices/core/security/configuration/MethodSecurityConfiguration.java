package fr.cnes.regards.microservices.core.security.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.ResourceAccessVoter;

/**
 * This class allow to add a security filter on method access.
 * Each time a method is called, the accessDecisionManager check if the connected user
 * can access the method via the ResourceAccessVoter class.
 * 
 * @author CS SI
 *
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfiguration extends GlobalMethodSecurityConfiguration {

	@Autowired
	private MethodAuthorizationService methodAuthService_;

	@Override
	protected AccessDecisionManager accessDecisionManager() {
		List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();
		decisionVoters.add(new ResourceAccessVoter(methodAuthService_));
		return new AffirmativeBased(decisionVoters);
	}
}
