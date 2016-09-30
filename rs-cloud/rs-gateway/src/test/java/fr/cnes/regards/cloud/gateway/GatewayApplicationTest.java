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

    // @Test
    public void contextLoads() {
        // The application can start with spring configuration
    }

    @Test
    public void testAuthenticate() {

        try {
            String basicString = basicUserName_ + ":" + basicPassword_;
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());

            String invalidBasicString = "invalid:invalid";
            invalidBasicString = Base64.getEncoder().encodeToString(invalidBasicString.getBytes());

            mockMvc.perform(get("/oauth/token")).andExpect(MockMvcResultMatchers.status().isUnauthorized());
            mockMvc.perform(post("/oauth/token")).andExpect(MockMvcResultMatchers.status().isUnauthorized());

            mockMvc.perform(post("/oauth/token").with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + invalidBasicString)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                    .param("grant_type", "password").param("scope", "plop").param("username", "plop")
                    .param("password", "plop")).andExpect(MockMvcResultMatchers.status().isUnauthorized());

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