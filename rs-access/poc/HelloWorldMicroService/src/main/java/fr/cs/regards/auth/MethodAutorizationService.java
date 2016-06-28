package fr.cs.regards.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;

public interface MethodAutorizationService {

	Optional<List<GrantedAuthority>> getAuthorities(ResourceAccess access);
}
