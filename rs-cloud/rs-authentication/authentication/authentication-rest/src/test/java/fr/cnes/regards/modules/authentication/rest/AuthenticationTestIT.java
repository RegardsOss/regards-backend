/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Base64;

/**
 * Class GatewayApplicationTest Test class for the Gateway application
 *
 * @author Sébastien Binda
 */
@SpringBootTest(classes = AuthenticationTestConfiguration.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@EnableAutoConfiguration
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=auth_it" })
public class AuthenticationTestIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationTestIT.class);

    /**
     * Token endpoint
     */
    private static final String TOKEN_ENDPOINT = "/oauth/token";

    /**
     * BAsic authentication string in HTTP header request
     */
    private static final String BASIC_AUTH = "Basic ";

    /**
     * Gran type label
     */
    private static final String GRANT_TYPE = "grant_type";

    /**
     * password label
     */
    private static final String PASSWORD = "password";

    /**
     * username label
     */
    private static final String USER_NAME = "username";

    /**
     * Scope label
     */
    private static final String SCOPE = "scope";

    /**
     * Basic authentication username
     */
    @Value("${regards.authentication.client.user}")
    private String basicUserName;

    /**
     * Basic authentication secret
     */
    @Value("${regards.authentication.client.secret}")
    private String basicPassword;

    @Value("${jwt.validityDelay:120}")
    private final long validityDelay = 120;

    /**
     * Spring Mock Mvc to simulare REST requests.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Check that the gateway spring context is valid
     */
    @Purpose("Check that the gateway spring context is valid")
    @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

    /**
     * Check access to unexisting endpoints. Response must be Unauthorized.
     */
    @Test
    public void test() throws Exception {
        String invalidBasicString = "invalid:invalid";
        invalidBasicString = Base64.getEncoder().encodeToString(invalidBasicString.getBytes());

        mockMvc.perform(MockMvcRequestBuilders.get("/non/existing/endpoint"))
               .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    /**
     * Test the Oauth2 authentication process. Test unauthorized for basic authentication fail.
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Test unauthorized for basic authentication fail.")
    @Test
    public void testAuthenticateBasicError() {
        try {
            String invalidBasicString = "invalid:invalid";
            invalidBasicString = Base64.getEncoder().encodeToString(invalidBasicString.getBytes());

            mockMvc.perform(MockMvcRequestBuilders.get(TOKEN_ENDPOINT))
                   .andExpect(MockMvcResultMatchers.status().isUnauthorized());
            mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT))
                   .andExpect(MockMvcResultMatchers.status().isUnauthorized());

            mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                                                  .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH + invalidBasicString)
                                                  .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                  .param(GRANT_TYPE, PASSWORD)
                                                  .param(SCOPE, "scope1")
                                                  .param(USER_NAME, "name1")
                                                  .param(PASSWORD, "mdp"))
                   .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the Oauth2 authentication process. Test unauthorized for user/password invalid.
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Test unauthorized for user/password invalid.")
    @Test
    public void testAuthenticateCredentialsError() {
        try {
            String basicString = String.format("%s:%s", basicUserName, basicPassword);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                                                  .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString)
                                                  .header(HttpHeaders.CONTENT_TYPE,
                                                          MediaType.APPLICATION_JSON_UTF8_VALUE)
                                                  .param(GRANT_TYPE, PASSWORD)
                                                  .param(SCOPE, "PROJECT")
                                                  .param(USER_NAME, "name2")
                                                  .param(PASSWORD, AuthenticationTestConfiguration.INVALID_PASSWORD))
                   .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        } catch (final Exception e) { // NOSONAR
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the Oauth2 authentication process. Get a valid token.
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Get a valid token.")
    @Test
    public void testAuthenticate() {
        try {
            String basicString = String.format("%s:%s", basicUserName, basicPassword);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                                                                         .header(HttpHeaders.AUTHORIZATION,
                                                                                 BASIC_AUTH + basicString)
                                                                         .header(HttpHeaders.CONTENT_TYPE,
                                                                                 MediaType.APPLICATION_JSON_VALUE)
                                                                         .param(GRANT_TYPE, PASSWORD)
                                                                         .param(SCOPE, "PROJECT")
                                                                         .param(USER_NAME, "test@regards.fr")
                                                                         .param(PASSWORD,
                                                                                AuthenticationTestConfiguration.VALID_PASSWORD))
                                          .andExpect(MockMvcResultMatchers.status().isOk())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.access_token").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.email").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.scope").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.tenant").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.sub").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.role").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.token_type").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.expires_in").exists());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the Oauth2 authentication process with payload. Get a valid token.
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process with payload. Get a valid token.")
    @Test
    public void testAuthenticateWithPayload() {
        try {
            String basicString = String.format("%s:%s", basicUserName, basicPassword);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
            TokenController.UserAuthentication userAuthentication = new TokenController.UserAuthentication(null,
                                                                                                           AuthenticationTestConfiguration.VALID_PASSWORD,
                                                                                                           "PROJECT",
                                                                                                           PASSWORD);
            String userAuthenticationAsString = new ObjectMapper().writeValueAsString(userAuthentication);
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                                                                         .header(HttpHeaders.AUTHORIZATION,
                                                                                 BASIC_AUTH + basicString)
                                                                         .header(HttpHeaders.CONTENT_TYPE,
                                                                                 MediaType.APPLICATION_JSON_VALUE)
                                                                         .param(USER_NAME, "test@regards.fr")
                                                                         .param(SCOPE, "PROJECT")
                                                                         .content(userAuthenticationAsString))
                                          .andExpect(MockMvcResultMatchers.status().isOk())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.access_token").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.email").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.scope").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.tenant").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.sub").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.role").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.token_type").exists())
                                          .andExpect(MockMvcResultMatchers.jsonPath("$.expires_in").exists());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test the Oauth2 authentication process with payload and without scope. Should fail.
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process with payload and without scope. Should fail")
    @Test
    public void testAuthenticateWithPayloadAndMissingScope() {
        try {
            String basicString = String.format("%s:%s", basicUserName, basicPassword);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
            TokenController.UserAuthentication userAuthentication = new TokenController.UserAuthentication(null,
                                                                                                           AuthenticationTestConfiguration.VALID_PASSWORD,
                                                                                                           "PROJECT",
                                                                                                           PASSWORD);
            String userAuthenticationAsString = new ObjectMapper().writeValueAsString(userAuthentication);
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                                                                         .header(HttpHeaders.AUTHORIZATION,
                                                                                 BASIC_AUTH + basicString)
                                                                         .header(HttpHeaders.CONTENT_TYPE,
                                                                                 MediaType.APPLICATION_JSON_VALUE)
                                                                         .param(USER_NAME, "test@regards.fr")
                                                                         .content(userAuthenticationAsString))
                                          .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}