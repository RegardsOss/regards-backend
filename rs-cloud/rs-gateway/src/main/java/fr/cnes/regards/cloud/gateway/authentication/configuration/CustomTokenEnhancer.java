/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import fr.cnes.regards.cloud.gateway.authentication.providers.SimpleAuthentication;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

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

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        final ProjectUser user = (ProjectUser) authentication.getUserAuthentication().getPrincipal();
        Set<String> scopes = authentication.getOAuth2Request().getScope();
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put(SimpleAuthentication.CLAIM_PROJECT, scopes.stream().findFirst().get());
        additionalInfo.put(SimpleAuthentication.CLAIM_EMAIL, user.getAccount().getEmail());
        additionalInfo.put(SimpleAuthentication.CLAIM_ROLE, user.getRole().getName());
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
        return accessToken;
    }
}
