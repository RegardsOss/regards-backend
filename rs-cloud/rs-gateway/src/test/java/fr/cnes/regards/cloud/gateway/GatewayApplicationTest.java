/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.cloud.gateway.authentication.provider.AuthenticationProviderStub;
import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
public class GatewayApplicationTest {

    @Value("${authentication.client.user}")
    private String basicUserName_;

    @Value("${authentication.client.secret}")
    private String basicPassword_;

    @Autowired
    private MockMvc mockMvc;

    @Purpose("Check that the gateway spring context is valid")
    @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Test unauthorized for basic authentication fail.")
    @Test
    public void testAuthenticateBadicError() {
        try {
            String invalidBasicString = "invalid:invalid";
            invalidBasicString = Base64.getEncoder().encodeToString(invalidBasicString.getBytes());

            mockMvc.perform(get("/oauth/token")).andExpect(MockMvcResultMatchers.status().isUnauthorized());
            mockMvc.perform(post("/oauth/token")).andExpect(MockMvcResultMatchers.status().isUnauthorized());

            mockMvc.perform(post("/oauth/token").with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + invalidBasicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param("grant_type", "password").param("scope", "plop").param("username", "plop")
                    .param("password", "plop")).andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }
        catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Test unauthorized for user/password invalid.")
    @Test
    public void testAuthenticateCredantialsError() {
        try {
            String basicString = basicUserName_ + ":" + basicPassword_;
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc.perform(post("/oauth/token").with(csrf()).header(HttpHeaders.AUTHORIZATION, "Basic " + basicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param("grant_type", "password").param("scope", "plop").param("username", "plop")
                    .param("password", AuthenticationProviderStub.INVALID_PASSWORD))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());
        }
        catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Purpose("Test the Oauth2 authentication process. Get a valid token.")
    @Test
    public void testAuthenticate() {
        try {
            String basicString = basicUserName_ + ":" + basicPassword_;
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            mockMvc.perform(post("/oauth/token").with(csrf()).header(HttpHeaders.AUTHORIZATION, "Basic " + basicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param("grant_type", "password").param("scope", "plop").param("username", "plop")
                    .param("password", "plop")).andExpect(MockMvcResultMatchers.status().isOk());
        }
        catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}