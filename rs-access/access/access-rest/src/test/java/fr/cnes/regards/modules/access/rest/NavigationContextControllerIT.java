/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.access.domain.ConfigParameter;
import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.access.domain.Project;
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
        expectations.add(MockMvcResultMatchers.jsonPath("$", hasSize(service.list().size())));

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
        final Long tinyUrlId = 3456L;

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        performDefaultGet(apiATinyUrl, expectations, "unable to load a specific context navigation", tinyUrlId);
    }

    /**
     * Update an URL
     */
    @Test
    public final void updateAnExistingUrl() {
        final NavigationContext navContext = service.list().get(0);
        final Long tinyUrlId = navContext.getId();

        navContext.setRoute("http:/localhost:port/webapps/newRoute");
        navContext.setStore(2468);
        navContext.setTinyUrl("sdjksjdklqsjdkljsdkljqskldjklqsjkjqdkljqdkljldjq/skljdklsjdkljs");
        navContext.addQueryParameters(new ConfigParameter("a new param", "a value"));

        final List<ResultMatcher> expectations = new ArrayList<>();

        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.id",
                                                        Matchers.hasToString(navContext.getId().toString())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.tinyUrl",
                                                        Matchers.hasToString(navContext.getTinyUrl())));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.route", Matchers.hasToString(navContext.getRoute())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.store",
                                                        Matchers.hasToString(navContext.getStore().toString())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.project.name",
                                                        Matchers.hasToString(navContext.getProject().getName())));
        expectations.add(MockMvcResultMatchers
                .jsonPath("$.content.project.theme.isDefault",
                          Matchers.hasToString(navContext.getProject().getTheme().isDefault().toString())));
        expectations.add(MockMvcResultMatchers
                .jsonPath("$.content.project.theme.themeType",
                          Matchers.hasToString(navContext.getProject().getTheme().getThemeType().toString())));
        expectations.add(MockMvcResultMatchers
                .jsonPath("$.content.queryParameters[4].name",
                          Matchers.hasToString(navContext.getQueryParameters().get(4).getName())));
        expectations.add(MockMvcResultMatchers
                .jsonPath("$.content.queryParameters[4].value",
                          Matchers.hasToString(navContext.getQueryParameters().get(4).getValue())));

        performDefaultPut(apiATinyUrl, navContext, expectations, "unable to update an existing context navigation",
                          tinyUrlId);
    }

    /**
     * Update an unknown URL
     */
    @Test
    public final void updateAnUnknownUrl() {
        final NavigationContext navContext = new NavigationContext(new Project(), null, "hello", 9876);
        final Long tinyUrlId = 0157L;
        navContext.setId(tinyUrlId);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        performDefaultPut(apiATinyUrl, navContext, expectations, "unable to update an existing context navigation",
                          tinyUrlId);
    }

    /**
     * Update an unknown URL
     */
    @Test
    public final void updateAnIncorrectUrl() {
        final NavigationContext navContext = service.list().get(0);
        final Long tinyUrlId = 333333L;

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        performDefaultPut(apiATinyUrl, navContext, expectations, "unable to update an existing context navigation",
                          tinyUrlId);
    }

    /**
     * Delete an existing URL
     */
    @Test
    public final void deleteAnExistingUrl() {
        final Long tinyUrlId = service.list().get(2).getId();

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());

        performDefaultDelete(apiATinyUrl, expectations, "unable to delete a specific context navigation", tinyUrlId);
    }

    /**
     * Delete an existing URL
     */
    @Test
    public final void deleteAnUnknownUrl() {
        final Long tinyUrlId = 876L;

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isNotFound());

        performDefaultDelete(apiATinyUrl, expectations, "unable to delete a specific context navigation", tinyUrlId);
    }

    /**
     * Create a new URL
     */
    @Test
    public final void saveANewUrl() {
        final NavigationContext navContext = new NavigationContext(new Project(), null, "http://tinyRegardsStart",
                9876);
        navContext.setId(9090L);
        navContext.setTinyUrl("http://AbGF1234");
        navContext.addQueryParameters(new ConfigParameter("a param", "a value"));
        navContext.addQueryParameters(new ConfigParameter("a second param", "a second value"));

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.tinyUrl",
                                                        Matchers.hasToString(navContext.getTinyUrl())));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.content.route", Matchers.hasToString(navContext.getRoute())));
        expectations.add(MockMvcResultMatchers.jsonPath("$.content.store",
                                                        Matchers.hasToString(navContext.getStore().toString())));
        expectations.add(status().isCreated());

        performDefaultPost(apiTinyUrls, navContext, expectations, "unable to create a new context navigation");
    }

    @Test
    public final void saveANullUrl() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isServiceUnavailable());

        performDefaultPost(apiTinyUrls, null, expectations, "unable to creta a new context navigation");
    }

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
