/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.access.service.INavigationContextService;

/**
 * @author Christophe Mertz
 *
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class NavigationContextControllerIT extends AbstractRegardsIT {

    @Autowired
    private INavigationContextService service;

    private final String apiTinyUrls = "/tiny/urls";

    private final String apiATinyUrl = "/tiny/url/{navCtxId}";

    /**
     * Get all URLs
     */
    @DirtiesContext
    @Test
    public final void getAllUrls() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultGet(apiTinyUrls, expectations, "unable to load all the context navigation");
    }

    /**
     * Get an existing URL
     */
    @Test
    public final void getAnExistingUrl() {
        final Long tinyUrlId = service.list().get(0).getId();

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        performDefaultGet(apiATinyUrl, expectations, "unable to load a specific context navigation", tinyUrlId);
    }
    
    /**
     * Get an unknown URL
     */
    @Test
    public final void getAnUnknownUrl() {
        final Long tinyUrlId = 0157L;

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().is4xxClientError());
        performDefaultGet(apiATinyUrl, expectations, "unable to load a specific context navigation", tinyUrlId);
    }

    // /**
    // * Update an existing URL
    // */
    // @Test
    // public final void dPutAnExistingUrl() {
    // NavigationContext aNavigationContext = getNavigationContext();
    //
    // try {
    // List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 1 ", "value 1"),
    // new ConfigParameter("param 2 ", "value 2"));
    // List<ConfigParameter> navCtxtParameters = Arrays
    // .asList(new ConfigParameter("param 1 ", "value 1"), new ConfigParameter("param 2 ", "value 2"),
    // new ConfigParameter("param 3 ", "value 3"), new ConfigParameter("param 4 ", "value 4"));
    // NavigationContext navigationContext = new NavigationContext(aNavigationContext.getTinyUrl(),
    // new Project("project1", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
    // "http:/localhost:port/webapps/url", 95);
    //
    // mvc_.perform(put("/tiny/url/" + aNavigationContext.getTinyUrl()).with(csrf())
    // .content(json(navigationContext)).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)
    // .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());
    // }
    // catch (Exception e) {
    // String message = "Cannot put an existing url:" + aNavigationContext.getTinyUrl();
    // LOG.error(message, e);
    // Assert.fail(message);
    // }
    // }
    //
    // /**
    // * Create a new URL
    // */
    // @Test
    // public final void ePostUrl() {
    //
    // List<ConfigParameter> themeParameters = Arrays.asList(new ConfigParameter("param 11 ", "value 11"),
    // new ConfigParameter("param 12 ", "value 12"));
    // List<ConfigParameter> navCtxtParameters = Arrays
    // .asList(new ConfigParameter("param 100 ", "value 100"), new ConfigParameter("param 101", "value 101"),
    // new ConfigParameter("param 103 ", "value 103"), new ConfigParameter("param 104 ", "value 104"));
    // NavigationContext aNavigationContext = new NavigationContext(
    // new Project("project2", new Theme(themeParameters, true, ThemeType.ALL)), navCtxtParameters,
    // "http:/localhost:port/webapps/newUrl", 133);
    // try {
    // mvc_.perform(post("/tiny/urls").with(csrf()).content(json(aNavigationContext))
    // .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)
    // .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk());
    // }
    // catch (Exception e) {
    // String message = "Cannot post an existing url:" + aNavigationContext.getTinyUrl();
    // LOG.error(message, e);
    // Assert.fail(message);
    // }
    // }
    //
    // /**
    // * Delete an existing URL
    // */
    // @Test
    // public final void fDeleteAnUrl() {
    // NavigationContext aNavigationContext = getNavigationContext();
    //
    // try {
    // mvc_.perform(delete("/tiny/url/" + aNavigationContext.getTinyUrl()).with(csrf())
    // .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().isOk());
    // }
    // catch (Exception e) {
    // String message = "Cannot delete the url:" + aNavigationContext;
    // LOG.error(message, e);
    // Assert.fail(message);
    // }
    // }
    //
    // /**
    // * Delete an unknown URL
    // */
    // @Test
    // public final void gDeleteAnUnknownUrl() {
    //
    // try {
    // mvc_.perform(delete("/tiny/url/" + "totutiti").with(csrf())
    // .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt_)).andExpect(status().is4xxClientError());
    // }
    // catch (Exception e) {
    // String message = "Cannot delete the url:" + AN_UNKNOWN_TINY_URL;
    // LOG.error(message, e);
    // Assert.fail(message);
    // }
    // }
    //
    // /**
    // * Get an existing NavigationContext
    // *
    // * @return a Navigation context element
    // */
    // private final NavigationContext getNavigationContext() {
    // return navigationContextService_.list().get(navigationContextService_.list().size() - 1);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.framework.test.integration.AbstractRegardsIT#getLogger()
     */
    @Override
    protected Logger getLogger() {
        // TODO Auto-generated method stub
        return null;
    }

}
