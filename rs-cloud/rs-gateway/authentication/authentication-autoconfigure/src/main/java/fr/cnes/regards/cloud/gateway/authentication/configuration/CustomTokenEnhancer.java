/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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
 * @author CS
 * @since 1.0-SNPASHOT
 */
public class CustomTokenEnhancer implements TokenEnhancer {

    /**
     * Security JWT service
     */
    @Autowired
    private JWTService jwtService;

    @Override
    public OAuth2AccessToken enhance(final OAuth2AccessToken pAccessToken, final OAuth2Authentication pAuthentication) {
        final UserDetails user = (UserDetails) pAuthentication.getUserAuthentication().getPrincipal();
        final Set<String> scopes = pAuthentication.getOAuth2Request().getScope();
        ((DefaultOAuth2AccessToken) pAccessToken).setAdditionalInformation(jwtService
                .generateClaims(scopes.stream().findFirst().get(), user.getEmail(), user.getRole(), user.getEmail()));
        return pAccessToken;
    }
}
