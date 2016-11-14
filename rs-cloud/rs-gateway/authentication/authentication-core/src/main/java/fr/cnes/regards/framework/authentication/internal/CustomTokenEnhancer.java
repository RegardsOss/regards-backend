/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;

/**
 *
 * Class CustomTokenEnhancer
 *
 * Add custom properties "CLAIMS" into each generated tokens
 *
 * @author Sébastien Binda
 * @since 1.0-SNPASHOT
 */
public class CustomTokenEnhancer implements TokenEnhancer {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CustomTokenEnhancer.class);

    /**
     * Security JWT service
     */
    private final JWTService jwtService;

    public CustomTokenEnhancer(final JWTService pJwtService) {
        super();
        jwtService = pJwtService;
    }

    @Override
    public OAuth2AccessToken enhance(final OAuth2AccessToken pAccessToken, final OAuth2Authentication pAuthentication) {
        final UserDetails user = (UserDetails) pAuthentication.getUserAuthentication().getPrincipal();
        final Set<String> scopes = pAuthentication.getOAuth2Request().getScope();
        if ((jwtService != null) && (user != null) && (scopes != null) && !scopes.isEmpty()) {
            ((DefaultOAuth2AccessToken) pAccessToken).setAdditionalInformation(jwtService
                    .generateClaims(scopes.stream().findFirst().get(), user.getEmail(), user.getRole(),
                                    user.getEmail()));
        } else {
            LOG.error("[OAUTH2 AUTHENTICATION] Error adding claims to JWT token.");
        }
        return pAccessToken;
    }
}
