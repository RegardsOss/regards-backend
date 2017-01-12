/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import java.util.Base64;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.cloud.gateway.authentication.stub.AuthenticationPluginStub;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class GatewayApplicationTest
 *
 * Test class for the Gateway application
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@SpringBootTest(classes = AuthenticationTestConfiguration.class)
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
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

    /**
     * Spring Mock Mvc to simulare REST requests.
     */
    @Autowired
    private MockMvc mockMvc;

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
     * Check access to unexisting endpoints. Response must be Unauthorized.
     *
     * @throws Exception
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void test() throws Exception {
        String invalidBasicString = "invalid:invalid";
        invalidBasicString = Base64.getEncoder().encodeToString(invalidBasicString.getBytes());

        mockMvc.perform(MockMvcRequestBuilders.get("/non/existing/endpoint"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
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
    @Ignore
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
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE).param(GRANT_TYPE, PASSWORD)
                    .param(SCOPE, "scope1").param(USER_NAME, "name1").param(PASSWORD, "mdp"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
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
    @Ignore
    public void testAuthenticateCredantialsError() {
        try {
            String basicString = String.format("%s:%s", basicUserName, basicPassword);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE).param(GRANT_TYPE, PASSWORD)
                    .param(SCOPE, "scope2").param(USER_NAME, "name2")
                    .param(PASSWORD, AuthenticationPluginStub.INVALID_PASSWORD))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
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
    @Ignore
    public void testAuthenticate() {
        try {
            String basicString = String.format("%s:%s", basicUserName, basicPassword);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc.perform(MockMvcRequestBuilders.post(TOKEN_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE).param(GRANT_TYPE, PASSWORD)
                    .param(SCOPE, "scope3").param(USER_NAME, "test@regards.fr").param(PASSWORD, "plop"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}