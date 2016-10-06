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

    /**
     * Security JWT service
     */
    @Autowired
    private JWTService jwtService_;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken pAccessToken, OAuth2Authentication pAuthentication) {
        final ProjectUser user = (ProjectUser) pAuthentication.getUserAuthentication().getPrincipal();
        final Set<String> scopes = pAuthentication.getOAuth2Request().getScope();
        ((DefaultOAuth2AccessToken) pAccessToken).setAdditionalInformation(jwtService_
                .generateClaims(scopes.stream().findFirst().get(), user.getAccount().getEmail(),
                                user.getRole().getName(), user.getAccount().getLogin()));
        return pAccessToken;
    }
}
