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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.test.AbstractRegardsIntegrationTest;
import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.domain.Project;
import fr.cnes.regards.modules.access.domain.Theme;
import fr.cnes.regards.modules.access.domain.ThemeType;
import fr.cnes.regards.modules.access.service.INavigationContextService;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * @author cmertz
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NavigationContextControllerIT extends AbstractRegardsIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationContextControllerIT.class);

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
    private MethodAuthorizationService methodAutorizationService;

    @Autowired
    private INavigationContextService navigationContextService_;

    /**
     * A not existing URL in the stub
     */
    private final String AN_UNKNOWN_TINY_URL = "ABCD12345";

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

    /**
     * Get all URLs
     */
    @Test
    public final void aGetAllUrls() {

        try {
            mvc_.perform(get("/tiny/urls").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_))
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

        NavigationContext aNavigationContext = getNavigationContext();

        try {
            mvc_.perform(get("/tiny/url/" + aNavigationContext.getTinyUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot get a specific url:" + aNavigationContext.getTinyUrl();
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
            mvc_.perform(get("/tiny/url/" + AN_UNKNOWN_TINY_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_))
                    .andExpect(status().is4xxClientError());
        }
        catch (Exception e) {
            String message = "Cannot get an unknown url:" + AN_UNKNOWN_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Update an existing URL
     */
    @Test
    public final void dPutAnExistingUrl() {
        NavigationContext aNavigationContext = getNavigationContext();

        try {
            List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 1 ", "value 1"),
                                                                  new ConfigParameter("param 2 ", "value 2"));
            List<ConfigParameter> navCtxtParameters = Arrays
                    .asList(new ConfigParameter("param 1 ", "value 1"), new ConfigParameter("param 2 ", "value 2"),
                            new ConfigParameter("param 3 ", "value 3"), new ConfigParameter("param 4 ", "value 4"));
            NavigationContext navigationContext = new NavigationContext(aNavigationContext.getTinyUrl(),
                    new Project("project1", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
                    "http:/localhost:port/webapps/url", 95);

            mvc_.perform(put("/tiny/url/" + aNavigationContext.getTinyUrl()).with(csrf())
                    .content(json(navigationContext)).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot put an existing url:" + aNavigationContext.getTinyUrl();
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Create a new URL
     */
    @Test
    public final void ePostUrl() {

        List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 11 ", "value 11"),
                                                              new ConfigParameter("param 12 ", "value 12"));
        List<ConfigParameter> navCtxtParameters = Arrays
                .asList(new ConfigParameter("param 100 ", "value 100"), new ConfigParameter("param 101", "value 101"),
                        new ConfigParameter("param 103 ", "value 103"), new ConfigParameter("param 104 ", "value 104"));
        NavigationContext aNavigationContext = new NavigationContext(
                new Project("project2", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
                "http:/localhost:port/webapps/newUrl", 133);
        try {
            mvc_.perform(post("/tiny/urls").with(csrf()).content(json(aNavigationContext))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot post an existing url:" + aNavigationContext.getTinyUrl();
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Delete an existing URL
     */
    @Test
    public final void fDeleteAnUrl() {
        NavigationContext aNavigationContext = getNavigationContext();

        try {
            mvc_.perform(delete("/tiny/url/" + aNavigationContext.getTinyUrl()).with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().isOk());
        }
        catch (Exception e) {
            String message = "Cannot delete the url:" + aNavigationContext;
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
            mvc_.perform(delete("/tiny/url/" + "totutiti").with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().is4xxClientError());
        }
        catch (Exception e) {
            String message = "Cannot delete the url:" + AN_UNKNOWN_TINY_URL;
            LOG.error(message, e);
            Assert.fail(message);
        }
    }

    /**
     * Get an existing NavigationContext
     *
     * @return a Navigation context element
     */
    private final NavigationContext getNavigationContext() {
        return navigationContextService_.list().get(navigationContextService_.list().size() - 1);
    }

}
