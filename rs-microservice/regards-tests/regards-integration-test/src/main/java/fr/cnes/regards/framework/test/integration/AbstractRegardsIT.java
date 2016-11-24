/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import java.util.List;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.test.DefaultTestConfiguration;
import fr.cnes.regards.framework.security.domain.HttpConstants;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 *
 * Base class to realize integration tests using JWT and MockMvc
 *
 * @author svissier
 * @author Sébastien Binda
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@ContextConfiguration(classes = { DefaultTestConfiguration.class })
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractRegardsIT {

    /**
     * Default tenant configured in application properties
     */
    protected static final String DEFAULT_TENANT = "PROJECT";

    /**
     * Default user
     */
    protected static final String DEFAULT_USER = "default_user";

    /**
     * Default user email
     */
    protected static final String DEFAULT_USER_EMAIL = "default_user@regards.fr";

    /**
     * Default user role
     */
    protected static final String DEFAULT_ROLE = "ROLE_DEFAULT";

    /**
     * JSON path for links in responses
     */
    protected static final String JSON_PATH_LINKS = "$.links";

    /**
     * JSON path for content in responses
     */
    protected static final String JSON_PATH_CONTENT = "$.content";

    /**
     * JSON path root in responses
     */
    protected static final String JSON_PATH_ROOT = "$";

    /**
     * URL Path separator
     */
    protected static final String URL_PATH_SEPARATOR = "/";

    // CHECKSTYLE:OFF
    /**
     * JWT service
     */
    @Autowired
    protected JWTService jwtService;

    /**
     * Authorization service method
     */
    @Autowired
    protected MethodAuthorizationService authService;
    // CHECKSTYLE:ON

    /**
     * Mock for MVC testing
     */
    @Autowired
    private MockMvc mvc;

    protected abstract Logger getLogger();

    protected ResultActions performGet(final String pUrlTemplate, final String pAuthenticationToken,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final Object... pUrlVariables) {
        return performRequest(pAuthenticationToken, HttpMethod.GET, pUrlTemplate, pMatchers, pErrorMessage,
                              pUrlVariables);
    }

    protected ResultActions performGet(final String pUrlTemplate, final String pAuthenticationToken,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final RequestParamBuilder pRequestParams,
            final Object... pUrlVariables) {
        return performRequest(pAuthenticationToken, HttpMethod.GET, pUrlTemplate, pMatchers, pErrorMessage,
                              pRequestParams, pUrlVariables);
    }

    protected ResultActions performPost(final String pUrlTemplate, final String pAuthenticationToken,
            final Object pContent, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final Object... pUrlVariables) {
        return performRequest(pAuthenticationToken, HttpMethod.POST, pUrlTemplate, pContent, pMatchers, pErrorMessage,
                              pUrlVariables);
    }

    protected ResultActions performPut(final String pUrlTemplate, final String pAuthenticationToken,
            final Object pContent, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final Object... pUrlVariables) {
        return performRequest(pAuthenticationToken, HttpMethod.PUT, pUrlTemplate, pContent, pMatchers, pErrorMessage,
                              pUrlVariables);
    }

    protected ResultActions performDelete(final String pUrlTemplate, final String pAuthenticationToken,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final Object... pUrlVariables) {
        return performRequest(pAuthenticationToken, HttpMethod.DELETE, pUrlTemplate, pMatchers, pErrorMessage,
                              pUrlVariables);
    }

    // Automatic default security management methods

    protected ResultActions performDefaultGet(final String pUrlTemplate, final List<ResultMatcher> pMatchers,
            final String pErrorMessage, final RequestParamBuilder pRequestParams, final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, RequestMethod.GET);
        return performGet(pUrlTemplate, jwt, pMatchers, pErrorMessage, pRequestParams, pUrlVariables);
    }

    protected ResultActions performDefaultGet(final String pUrlTemplate, final List<ResultMatcher> pMatchers,
            final String pErrorMessage, final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, RequestMethod.GET);
        return performGet(pUrlTemplate, jwt, pMatchers, pErrorMessage, pUrlVariables);
    }

    protected ResultActions performDefaultPost(final String pUrlTemplate, final Object pContent,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, RequestMethod.POST);
        return performPost(pUrlTemplate, jwt, pContent, pMatchers, pErrorMessage, pUrlVariables);
    }

    protected ResultActions performDefaultPut(final String pUrlTemplate, final Object pContent,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, RequestMethod.PUT);
        return performPut(pUrlTemplate, jwt, pContent, pMatchers, pErrorMessage, pUrlVariables);
    }

    protected ResultActions performDefaultDelete(final String pUrlTemplate, final List<ResultMatcher> pMatchers,
            final String pErrorMessage, final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, RequestMethod.DELETE);
        return performDelete(pUrlTemplate, jwt, pMatchers, pErrorMessage, pUrlVariables);
    }

    /**
     * Perform a REST request and control expectations
     *
     * @param pAuthenticationToken
     *            JWT token
     * @param pHttpMethod
     *            HTTP method
     * @param pUrlTemplate
     *            URL template
     * @param pContent
     *            content for {@link HttpMethod#POST} and {@link HttpMethod#PUT} methods
     * @param pMatchers
     *            expectations
     * @param pErrorMessage
     *            message if error occurs
     * @param pUrlVariables
     *            URL variables
     * @return result
     */
    protected ResultActions performRequest(final String pAuthenticationToken, final HttpMethod pHttpMethod,
            final String pUrlTemplate, final Object pContent, final List<ResultMatcher> pMatchers,
            final String pErrorMessage, final Object... pUrlVariables) {

        Assert.assertTrue(HttpMethod.POST.equals(pHttpMethod) || HttpMethod.PUT.equals(pHttpMethod));
        MockHttpServletRequestBuilder requestBuilder = getRequestBuilder(pAuthenticationToken, pHttpMethod,
                                                                         pUrlTemplate, pUrlVariables);
        final String content = gson(pContent);
        requestBuilder = requestBuilder.content(content).header(HttpHeaders.CONTENT_TYPE,
                                                                MediaType.APPLICATION_JSON_VALUE);
        return performRequest(requestBuilder, pMatchers, pErrorMessage);
    }

    protected ResultActions performRequest(final String pAuthenticationToken, final HttpMethod pHttpMethod,
            final String pUrlTemplate, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final RequestParamBuilder pRequestParams, final Object... pUrlVariables) {

        // Request parameters is only available on GET request AT THE MOMENT
        Assert.assertTrue(HttpMethod.GET.equals(pHttpMethod));
        final MockHttpServletRequestBuilder requestBuilder = getRequestBuilder(pAuthenticationToken, pHttpMethod,
                                                                               pUrlTemplate, pUrlVariables);
        requestBuilder.params(pRequestParams.getParameters());
        return performRequest(requestBuilder, pMatchers, pErrorMessage);
    }

    protected ResultActions performRequest(final String pAuthenticationToken, final HttpMethod pHttpMethod,
            final String pUrlTemplate, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final Object... pUrlVariables) {

        Assert.assertTrue(HttpMethod.GET.equals(pHttpMethod) || HttpMethod.DELETE.equals(pHttpMethod));
        final MockHttpServletRequestBuilder requestBuilder = getRequestBuilder(pAuthenticationToken, pHttpMethod,
                                                                               pUrlTemplate, pUrlVariables);
        return performRequest(requestBuilder, pMatchers, pErrorMessage);
    }

    /**
     *
     * @param pRequestBuilder
     *            request builder
     * @param pMatchers
     *            expectations
     * @param pErrorMessage
     *            message if error occurs
     * @return result
     */
    protected ResultActions performRequest(final MockHttpServletRequestBuilder pRequestBuilder,
            final List<ResultMatcher> pMatchers, final String pErrorMessage) {
        try {
            ResultActions request = mvc.perform(pRequestBuilder);
            for (final ResultMatcher matcher : pMatchers) {
                request = request.andExpect(matcher);
            }
            return request;
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            getLogger().error(pErrorMessage, e);
            throw new AssertionError(pErrorMessage + ": " + e);
        }
    }

    protected MockHttpServletRequestBuilder getRequestBuilder(final String pAuthToken, final HttpMethod pHttpMethod,
            final String pUrlTemplate, final Object... pUrlVars) {
        return MockMvcRequestBuilders.request(pHttpMethod, pUrlTemplate, pUrlVars)
                .header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + pAuthToken);
    }

    /**
     * Extract payload data from response
     *
     * @param pResultActions
     *            results
     * @return payload data
     */
    protected String payload(final ResultActions pResultActions) {
        Assert.assertNotNull(pResultActions);
        final MockHttpServletResponse response = pResultActions.andReturn().getResponse();
        try {
            final MediaType current = MediaType.parseMediaType(response.getContentType());
            if (current.getSubtype().contains("json")) {
                return response.getContentAsString();
            } else {
                throw new AssertionError("Invalid media type " + current);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            getLogger().error("Cannot parse payload data");
            throw new AssertionError(e);
        }
    }

    // CHECKSTYLE:OFF
    protected String gson(final Object pObject) {
        if (pObject instanceof String) {
            return (String) pObject;
        }
        final Gson gson = new Gson();
        return gson.toJson(pObject);
    }
    // CHECKSTYLE:ON

    /**
     * Generate token for default tenant
     *
     * @param pEmail
     *            user email
     * @param pName
     *            user name
     * @param pRole
     *            user role
     * @return JWT
     */
    protected String generateToken(final String pEmail, final String pName, final String pRole) {
        return jwtService.generateToken(DEFAULT_TENANT, pEmail, pName, pRole);
    }

    /**
     * Set authorities for default tenant
     *
     * @param pUrlPath
     *            endpoint
     * @param pMethod
     *            HTTP method
     * @param pRoleNames
     *            list of roles
     */
    protected void setAuthorities(final String pUrlPath, final RequestMethod pMethod, final String... pRoleNames) {
        authService.setAuthorities(DEFAULT_TENANT, pUrlPath, pMethod, pRoleNames);
    }

    /**
     * Helper method to manage default security with :
     * <ul>
     * <li>a default user</li>
     * <li>a default role</li>
     * </ul>
     * The helper generates a JWT using its default configuration and grants access to the endpoint for the default
     * role.
     *
     * @param pUrlPath
     *            target endpoint
     * @param pMethod
     *            target HTTP method
     * @return security token to authenticate user
     */
    protected String manageDefaultSecurity(final String pUrlPath, final RequestMethod pMethod) {
        final String jwt = generateToken(DEFAULT_USER_EMAIL, DEFAULT_USER, DEFAULT_ROLE);
        setAuthorities(pUrlPath, pMethod, DEFAULT_ROLE);
        return jwt;
    }

    protected static MultiValueMap<String, String> buildRequestParams() {
        return new LinkedMultiValueMap<String, String>();
    }
}
