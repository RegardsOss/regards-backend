/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.cloud.gateway.authentication.provider.AuthenticationProviderStub;
import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;

/**
 *
 * Class GatewayApplicationTest
 *
 * Test class for the Gateway application
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public class GatewayApplicationTest {

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
    @Value("${authentication.client.user}")
    private String basicUserName_;

    /**
     * Basic authentication secret
     */
    @Value("${authentication.client.secret}")
    private String basicPassword_;

    /**
     * Spring Mock Mvc to simulare REST requests.
     */
    @Autowired
    private MockMvc mockMvc_;

    /**
     *
     * Check that the gateway spring context is valid
     *
     * @since 1.0-SNAPSHOT
     */
    @Purpose("Check that the gateway spring context is valid")
    @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

    /**
     *
     * Test the Oauth2 authentication process. Test unauthorized for basic authentication fail.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Test unauthorized for basic authentication fail.")
    @Test
    public void testAuthenticateBadicError() {
        try {
            String invalidBasicString = "invalid:invalid";
            invalidBasicString = Base64.getEncoder().encodeToString(invalidBasicString.getBytes());

            mockMvc_.perform(MockMvcRequestBuilders.get(TOKEN_ENDPOINT)).andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());
            mockMvc_.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)).andExpect(MockMvcResultMatchers.status()
                    .isUnauthorized());

            mockMvc_.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT).with(SecurityMockMvcRequestPostProcessors
                    .csrf())
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH + invalidBasicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param(GRANT_TYPE, PASSWORD).param(SCOPE, "scope1").param(USER_NAME, "name1")
                    .param(PASSWORD, "mdp")).andExpect(MockMvcResultMatchers.status().isUnauthorized());
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Test the Oauth2 authentication process. Test unauthorized for user/password invalid.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Test unauthorized for user/password invalid.")
    @Test
    public void testAuthenticateCredantialsError() {
        try {
            String basicString = String.format("%s:%s", basicUserName_, basicPassword_);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc_.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT).with(SecurityMockMvcRequestPostProcessors
                    .csrf()).header(HttpHeaders.AUTHORIZATION,
                                    BASIC_AUTH
                                            + basicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param(GRANT_TYPE, PASSWORD).param(SCOPE, "scope2").param(USER_NAME, "name2")
                    .param(PASSWORD, AuthenticationProviderStub.INVALID_PASSWORD))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Test the Oauth2 authentication process. Get a valid token.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Get a valid token.")
    @Test
    public void testAuthenticate() {
        try {
            String basicString = basicUserName_ + ":" + basicPassword_;
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc_.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT).with(SecurityMockMvcRequestPostProcessors
                    .csrf()).header(HttpHeaders.AUTHORIZATION,
                                    BASIC_AUTH
                                            + basicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param(GRANT_TYPE, PASSWORD).param(SCOPE, "scope3").param(USER_NAME, "name3")
                    .param(PASSWORD, "plop")).andExpect(MockMvcResultMatchers.status().isOk());
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}