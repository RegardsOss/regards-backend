package fr.cnes.regards.microservices.core.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Interface for Method authorization access service
 * 
 * @author CS SI
 *
 */
public interface MethodAutorizationService {

	Optional<List<GrantedAuthority>> getAuthorities(RequestMapping access, RequestMapping classMapping);
	
	void setAutorities(String resourceName, GrantedAuthority... authorities);
}
