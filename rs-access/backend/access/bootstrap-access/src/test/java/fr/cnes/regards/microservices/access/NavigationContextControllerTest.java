/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.access;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.jwt.JWTService;
import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.domain.Project;
import fr.cnes.regards.modules.access.domain.Theme;
import fr.cnes.regards.modules.access.domain.ThemeType;

/**
 * @author cmertz
 *
 */
@ActiveProfiles("dev")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NavigationContextControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationContextControllerTest.class);

    private String jwt_;

    @Autowired
    private MockMvc mvc_;

    @Autowired
    private JWTService jwtService_;

    /**
     * 
     */
    private static boolean initialize = true;

    @Autowired
    private MethodAutorizationService methodAutorizationService;

    /**
     * An existing URL defined in the stub
     */
    private final String A_TINY_URL = "AbcD12345";

    /**
     * A not existing URL in the stub
     */
    private final String AN_UNKNOWN_TINY_URL = "ABCD12345";
    // ""

    /**
     * An existing URL to delete
     */
    private final String A_TINY_URL_2_DELETE = "dMLKMLK5454";

    @Before
    public final void setup() {
        jwt_ = jwtService_.generateToken("PROJECT", "email", "CMZ", "USER");
        if (!initialize) {
            return;
        }
        initialize = false;
        methodAutorizationService.setAuthorities("/tiny/urls", RequestMethod.GET, "USER");
        methodAutorizationService.setAuthorities("/tiny/urls", RequestMethod.POST, "USER");
        methodAutorizationService.setAuthorities("/tiny/url/{tinyUrl}", RequestMethod.GET, "USER");
        methodAutorizationService.setAuthorities("/tiny/url/{tinyUrl}", RequestMethod.PUT, "USER");
        methodAutorizationService.setAuthorities("/tiny/url/{tinyUrl}", RequestMethod.DELETE, "USER");
    }

    private HttpMessageConverter mappingJackson2HttpMessageConverter_;

    /**
     * 
     * @param converters
     */
    @Autowired
    final void setConverters(HttpMessageConverter<?>[] converters) {

        mappingJackson2HttpMessageConverter_ = Arrays.asList(converters).stream()
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().get();

        Assert.assertNotNull("the JSON message converter must not be null", mappingJackson2HttpMessageConverter_);
    }

    /**
     * Convert an object to a Json
     * 
     * @param pObj
     *            an Object
     * @throws IOException
     *             IOException
     * @return Json's object
     */
    private String json(Object pObj) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter_.write(pObj, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    /**
     * Get all URLs
     */
    @Test
    public final void aGetAllUrls() {

        try {
            this.mvc_.perform(get("/tiny/urls").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_))
                    .andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot get all urls";
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Get an existing URL
     */
    @Test
    public final void bGetAnExistingUrl() {

        try {
            this.mvc_.perform(get("/tiny/url/" + this.A_TINY_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_))
                    .andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot get a specific url:" + this.A_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Get an unknown URL
     */
    @Test
    public final void cGetAnUnknownUrl() {

        try {
            this.mvc_
                    .perform(get("/tiny/url/" + this.AN_UNKNOWN_TINY_URL).header(HttpHeaders.AUTHORIZATION,
                                                                                 "Bearer " + jwt_))
                    .andExpect(status().is4xxClientError());
        }
        catch (Exception e) {
            String message = "Cannot get an unknown url:" + this.AN_UNKNOWN_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Update an existing URL
     */
    @Test
    public final void dPutAnExistingUrl() {

        try {
            List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 1 ", "value 1"),
                                                                  new ConfigParameter("param 2 ", "value 2"));
            List<ConfigParameter> navCtxtParameters = Arrays
                    .asList(new ConfigParameter("param 1 ", "value 1"), new ConfigParameter("param 2 ", "value 2"),
                            new ConfigParameter("param 3 ", "value 3"), new ConfigParameter("param 4 ", "value 4"));
            NavigationContext navigationContext = new NavigationContext(this.A_TINY_URL,
                    new Project("project1", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
                    "http:/localhost:port/webapps/url", 95);

            this.mvc_
                    .perform(put("/tiny/url/" + this.A_TINY_URL).with(csrf()).content(json(navigationContext))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_).header(HttpHeaders.CONTENT_TYPE,
                                                                                        MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot put an existing url:" + this.A_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Create a new URL
     */
    @Test
    public final void ePostUrl() {

        try {
            List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 11 ", "value 11"),
                                                                  new ConfigParameter("param 12 ", "value 12"));
            List<ConfigParameter> navCtxtParameters = Arrays.asList(new ConfigParameter("param 100 ", "value 100"),
                                                                    new ConfigParameter("param 101", "value 101"),
                                                                    new ConfigParameter("param 103 ", "value 103"),
                                                                    new ConfigParameter("param 104 ", "value 104"));
            NavigationContext navigationContext = new NavigationContext(
                    new Project("project2", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
                    "http:/localhost:port/webapps/newUrl", 133);

            this.mvc_
                    .perform(post("/tiny/urls").with(csrf()).content(json(navigationContext))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot post an existing url:" + this.A_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Delete an existing URL
     */
    @Test
    public final void fDeleteAnUrl() {

        try {
            this.mvc_.perform(delete("/tiny/url/" + this.A_TINY_URL_2_DELETE).with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot delete the url:" + this.A_TINY_URL_2_DELETE;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Delete an unknown URL
     */
    @Test
    public final void gDeleteAnUnknownUrl() {

        try {
            this.mvc_.perform(delete("/tiny/url/" + "totutiti").with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().is4xxClientError());
        }
        catch (Exception e) {
            String message = "Cannot delete the url:" + this.AN_UNKNOWN_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

}
