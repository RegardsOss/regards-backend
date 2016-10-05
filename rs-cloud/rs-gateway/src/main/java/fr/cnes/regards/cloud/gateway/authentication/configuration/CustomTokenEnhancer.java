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

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.security.utils.jwt.JWTService;

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

    @Autowired
    private JWTService jwtService_;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        final ProjectUser user = (ProjectUser) authentication.getUserAuthentication().getPrincipal();
        Set<String> scopes = authentication.getOAuth2Request().getScope();
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(jwtService_
                .generateClaims(scopes.stream().findFirst().get(), user.getAccount().getEmail(),
                                user.getRole().getName(), user.getAccount().getLogin()));
        return accessToken;
    }
}
