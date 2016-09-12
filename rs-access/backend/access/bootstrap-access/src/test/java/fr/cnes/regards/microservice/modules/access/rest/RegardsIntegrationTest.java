/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 28/04/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.microservice.modules.access.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.microservices.access.Application;

/**
 *
 * Integration tests base class
 *
 * @author msordi
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public abstract class RegardsIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsIntegrationTest.class);

    private static final String BASE_URL_TEMPLATE = "http://localhost:%s";

    private static final String URL_SEPARATOR = "/";

    private static final String DEFAULT_CLIENT = "client";

    private static final String DEFAULT_CLIENT_SECRET = "secret";

    private static final String DEFAULT_SCOPE = "test";

    @Value("${server.port:3334}")
    private int port_;

    /**
     *
     * Build OAuth2 token endpoint
     *
     * @return OAuth2 token endpoint
     * @since 1.0-SNAPSHOT
     */
    private String getOAuth2TokenEndpoint() {
        return String.format(BASE_URL_TEMPLATE, port_ + "/oauth/token");
    }

    /**
     *
     * @return API endpoint with a trailing slash
     * @since 1.0-SNAPSHOT
     */
    protected String getApiEndpoint() {
        return String.format(BASE_URL_TEMPLATE, port_ + URL_SEPARATOR);
    }
    //
    // /**
    // * Build LEGACY endpoint for Restlet compatible modules
    // *
    // * @return LEGACY endpoint with a trailing slash
    // * @since 1.0-SNAPSHOT
    // */
    // protected String getLegacyEndpoint() {
    // return String.format(BASE_URL_TEMPLATE,
    // port_ + URL_SEPARATOR + ResourceServerConfigurer.REST_LEGACY_API_MAPPING);
    // }

    /**
     *
     * Get an OAuth2 token form embedded OAuth2 server.
     *
     * @param pClient
     *            client
     * @param pClientSecret
     *            client secret
     * @param pUsername
     *            username
     * @param pPassword
     *            password
     * @param pScope
     *            scope (i.e. project)
     * @return a Bearer token
     * @since 1.0-SNAPSHOT
     */
    private String getBearerToken(String pClient, String pClientSecret, String pUsername, String pPassword,
            String pScope) {

        // Client use a basic authentication in Spring OAuth2
        final TestRestTemplate restTemplate = new TestRestTemplate(pClient, pClientSecret);

        // MultiValueMap is converted by FormHttpMessageConverter
        // with a media type : application/x-www-form-urlencoded (see Spring doc)
        final MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
        bodyMap.add("grant_type", "password");
        bodyMap.add("username", pUsername);
        bodyMap.add("password", pPassword);
        bodyMap.add("scope", pScope);

        final ResponseEntity<OAuth2AccessToken> response = restTemplate.postForEntity(getOAuth2TokenEndpoint(), bodyMap,
                                                                                      OAuth2AccessToken.class);

        if (!HttpStatus.OK.equals(response.getStatusCode())) {
            Assert.fail(String.format("Fail to get a token! (http code : %s)", response.getStatusCode()));

        }

        return response.getBody().getValue();
    }

    /**
     *
     * Build a <b>new</b> {@link RestTemplate} with OAuth2 authentication features enabled.
     *
     * @param pUsername
     *            username
     * @param pPassword
     *            password
     * @return a new rest template
     * @since 1.0-SNAPSHOT
     */
    protected TestRestTemplate buildOauth2RestTemplate(String pUsername, String pPassword) {
        return buildOauth2RestTemplate(null, null, pUsername, pPassword, null);
    }

    /**
     *
     * Build a <b>new</b> {@link RestTemplate} with OAuth2 authentication features enabled.
     *
     * @param pUsername
     *            username
     * @param pPassword
     *            password
     * @param pScope
     *            scope or <code>null</code>
     * @return a new rest template
     * @since 1.0-SNAPSHOT
     */
    protected TestRestTemplate buildOauth2RestTemplate(String pUsername, String pPassword, String pScope) {
        return buildOauth2RestTemplate(null, null, pUsername, pPassword, pScope);
    }

    /**
     *
     * Build a <b>new</b> {@link RestTemplate} with OAuth2 authentication features enabled
     *
     * @param pClient
     *            client or <code>null</code>
     * @param pClientSecret
     *            client secret or <code>null</code>
     * @param pUsername
     *            username
     * @param pPassword
     *            password
     * @param pScope
     *            scope or <code>null</code>
     * @return a new rest template
     * @since 1.0-SNAPSHOT
     */
    protected TestRestTemplate buildOauth2RestTemplate(String pClient, String pClientSecret, String pUsername,
            String pPassword, String pScope) {

        Assert.assertNotNull("Username must not be null", pUsername);
        Assert.assertNotNull("Password must not be null", pPassword);

        String client = pClient, clientSecret = pClientSecret, scope = pScope;
        if (client == null) {
            LOGGER.info(String.format("Setting default client \"%s\"", DEFAULT_CLIENT));
            client = DEFAULT_CLIENT;
        }
        if (clientSecret == null) {
            LOGGER.info(String.format("Setting default client secret \"%s\"", DEFAULT_CLIENT_SECRET));
            clientSecret = DEFAULT_CLIENT_SECRET;
        }
        if (scope == null) {
            LOGGER.info(String.format("Setting default scope \"%s\"", DEFAULT_SCOPE));
            scope = DEFAULT_SCOPE;
        }

        // Init template
        final TestRestTemplate restTemplate = new TestRestTemplate();
        // Get bearer
        final String bearerToken = getBearerToken(client, clientSecret, pUsername, pPassword, scope);
        // Set interceptor to inject bearer
        final List<ClientHttpRequestInterceptor> interceptors = Collections
                .<ClientHttpRequestInterceptor> singletonList(new OAuth2AuthorizationInterceptor(bearerToken));
        restTemplate.getRestTemplate().setInterceptors(interceptors);

        return restTemplate;
    }

    /**
     *
     *
     * Http interceptor to inject OAuth2 authentication in {@link RestTemplate}
     *
     * @author msordi
     * @since 1.0-SNAPSHOT
     */
    private static class OAuth2AuthorizationInterceptor implements ClientHttpRequestInterceptor {

        private final String bearerToken_;

        public OAuth2AuthorizationInterceptor(String pBearerToken) {
            this.bearerToken_ = pBearerToken;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest pRequest, byte[] pBody, ClientHttpRequestExecution pExecution)
                throws IOException {
            pRequest.getHeaders().add("Authorization", "Bearer " + bearerToken_);
            return pExecution.execute(pRequest, pBody);
        }
    }
}
