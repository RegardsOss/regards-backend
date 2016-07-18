package fr.cnes.regards.microservices.core.auth;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class MethodAutorizationServiceImpl implements MethodAutorizationService {

	Map<String, List<GrantedAuthority>> grantedAuthoritiesByResource;

	public MethodAutorizationServiceImpl() {
		grantedAuthoritiesByResource = new HashMap<>();
		setAutorities("ME@GET", new RoleAuthority("MSI"), new RoleAuthority("USER"));
	}

	public void setAutorities(String resourceName, GrantedAuthority... authorities) {
		if (resourceName != null && authorities != null) {
			grantedAuthoritiesByResource.put(resourceName, Arrays.asList(authorities));
		}
	}

	@Override
	public Optional<List<GrantedAuthority>> getAuthorities(final ResourceAccess access) {
		return Optional.ofNullable(grantedAuthoritiesByResource.get(ResourceAccessUtils.getIdentifier(access)));
	}
}
