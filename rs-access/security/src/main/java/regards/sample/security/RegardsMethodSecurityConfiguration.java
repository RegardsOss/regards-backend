package regards.sample.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import regards.sample.security.auth.MethodAutorizationService;
import regards.sample.security.auth.ResourceAccessVoter;

@EnableGlobalMethodSecurity(prePostEnabled = true)
public class RegardsMethodSecurityConfiguration extends GlobalMethodSecurityConfiguration {

	@Autowired
	private MethodAutorizationService auth;
	
	@Autowired
	public void registerGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser("user").password("user").roles("USER").and().withUser("msi")
				.password("msi").roles("MSI");
	}

	@Override
	protected AccessDecisionManager accessDecisionManager() {
		List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();
		decisionVoters.add(new ResourceAccessVoter(auth));
		return new AffirmativeBased(decisionVoters);
	}
}
