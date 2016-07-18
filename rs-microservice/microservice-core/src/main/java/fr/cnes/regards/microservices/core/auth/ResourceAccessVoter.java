package fr.cnes.regards.microservices.core.auth;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class ResourceAccessVoter implements AccessDecisionVoter<Object> {

	private MethodAutorizationService authService;

	public ResourceAccessVoter(MethodAutorizationService authService) {
		this.authService = authService;
	}

	@Override
	public boolean supports(ConfigAttribute attribute) {
		// TODO
		return true;
	}

	/**
	 * This implementation supports any type of class, because it does not query
	 * the presented secure object.
	 *
	 * @param clazz
	 *            the secure object
	 *
	 * @return always <code>true</code>
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return true;
	}

	@Override
	public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {

		if (object instanceof MethodInvocation) {
			MethodInvocation mi = (MethodInvocation) object;
			ResourceAccess access = mi.getMethod().getAnnotation(ResourceAccess.class);

			Optional<List<GrantedAuthority>> option = authService.getAuthorities(access);
			
			if (option.isPresent()) {
				// All user authorities
				Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();

				if (userAuthorities != null) {
					// Check if user has correct authority
					for (GrantedAuthority userAuthority : userAuthorities) {
						for (GrantedAuthority resourceAuthority : option.get()) {
							if (userAuthority.getAuthority().equals(resourceAuthority.getAuthority())) {
								return ACCESS_GRANTED;
							}
						}
					}
				}

			}
		}
		// Default behaviour
		return ACCESS_DENIED;
	}

}
