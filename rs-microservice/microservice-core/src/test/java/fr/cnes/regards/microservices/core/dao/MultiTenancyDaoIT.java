/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.jwt.JWTService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoITConfiguration.class })
@WebAppConfiguration
@AutoConfigureMockMvc
@DirtiesContext
public class MultiTenancyDaoIT {

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAutorizationService authService_;

    @Test
    public void testMvc() {

        String tokenTest1 = jwtService_.generateToken("test1", "seb", "seb", "ADMIN");
        String tokenTest2 = jwtService_.generateToken("invalid", "seb", "seb", "ADMIN");
        authService_.setAuthorities("/test/dao/users", RequestMethod.GET, "ADMIN");
        authService_.setAuthorities("/test/dao/user", RequestMethod.POST, "ADMIN");

        try {

            // Run with valid tenant
            mockMvc.perform(get("/test/dao/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTest1))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            // Run with invalid tenant
            mockMvc.perform(get("/test/dao/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTest2))
                    .andExpect(MockMvcResultMatchers.status().isInternalServerError());

        }
        catch (Exception e) {
            Assert.fail(e.getStackTrace().toString());
        }
    }

}
