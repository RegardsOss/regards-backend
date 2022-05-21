/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.authentication;

import fr.cnes.regards.framework.authentication.internal.CustomTokenEnhancer;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.util.HashSet;
import java.util.Set;

/**
 * Class TokenEnhancerTest
 * <p>
 * Test utility adding specific REGARDS Claims in JWT token after authentication succeed by Oauth2 process
 *
 * @author Sébastien Binda
 */
public class TokenEnhancerTest {

    /**
     * Email to add in JWT Token
     */
    private static final String EMAIL = "test@regards.fr";

    /**
     * Project to add in JWT Token
     */
    private static final String PROJECT = "project";

    /**
     * Role to add in JWT Token
     */
    private static final String ROLE = "test";

    /**
     * Check that valid claims are added in JWT authentication token
     */
    @Purpose("Check access to gateway is not denied by the ROLE Filter for all non SYS roles users")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testAddCustomClaimsToJWTToken() {

        final JWTService service = new JWTService();
        service.setSecret("1234546789");

        final UserDetails details = new UserDetails(PROJECT, EMAIL, EMAIL, ROLE);

        final DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(Mockito.mock(OAuth2AccessToken.class));
        final OAuth2Authentication auth = Mockito.mock(OAuth2Authentication.class);
        Mockito.when(auth.getUserAuthentication()).thenReturn(Mockito.mock(Authentication.class));
        Mockito.when(auth.getUserAuthentication().getPrincipal()).thenReturn(details);
        final Set<String> scopes = new HashSet<>();
        scopes.add(PROJECT);
        final OAuth2Request mock = new OAuth2Request(null, null, null, false, scopes, null, null, null, null);
        Mockito.when(auth.getOAuth2Request()).thenReturn(mock);

        final CustomTokenEnhancer enhancer = new CustomTokenEnhancer(service);

        final OAuth2AccessToken result = enhancer.enhance(token, auth);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getAdditionalInformation().get(JWTService.CLAIM_TENANT), PROJECT);
        Assert.assertEquals(result.getAdditionalInformation().get(JWTService.CLAIM_ROLE), ROLE);
        Assert.assertEquals(result.getAdditionalInformation().get(JWTService.CLAIM_SUBJECT), EMAIL);
        Assert.assertEquals(result.getAdditionalInformation().get(JWTService.CLAIM_EMAIL), EMAIL);

    }

}
