package fr.cnes.regards.microservices.core.auth;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
public class MethodAutorizationServiceImpl implements MethodAutorizationService {

	Map<String, List<GrantedAuthority>> grantedAuthoritiesByResource;

	public MethodAutorizationServiceImpl() {
		grantedAuthoritiesByResource = new HashMap<>();
	}

	public void setAutorities(String resourceName, GrantedAuthority... authorities) {
		if (resourceName != null && authorities != null) {
			grantedAuthoritiesByResource.put(resourceName, Arrays.asList(authorities));
		}
	}

	@Override
	public Optional<List<GrantedAuthority>> getAuthorities(final RequestMapping access, RequestMapping classMapping) {
		String resourceId = ResourceAccessUtils.getIdentifier(access, classMapping);
		System.out.println("Accessing ressource id="+resourceId);
		return Optional.ofNullable(grantedAuthoritiesByResource.get(resourceId));
	}
}
