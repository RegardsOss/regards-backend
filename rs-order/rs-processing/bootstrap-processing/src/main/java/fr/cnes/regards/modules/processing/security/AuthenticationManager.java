package fr.cnes.regards.modules.processing.security;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JWTService jwtService;

    @Autowired
    public AuthenticationManager(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (JWTAuthentication.class.isInstance(authentication)) {
            return Mono.fromCallable(() -> jwtService.parseToken((JWTAuthentication)authentication));
        }
        else {
            return Mono.empty();
        }
    }
}
