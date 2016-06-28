package fr.cs.regards.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import fr.cs.regards.auth.MethodAutorizationService;
import fr.cs.regards.auth.ResourceAccessVoter;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class RegardsMethodSecurityConfiguration extends GlobalMethodSecurityConfiguration {

	@Autowired
	private MethodAutorizationService auth;

	@Override
	protected AccessDecisionManager accessDecisionManager() {
		List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();
		decisionVoters.add(new ResourceAccessVoter(auth));
		return new AffirmativeBased(decisionVoters);
	}
}
