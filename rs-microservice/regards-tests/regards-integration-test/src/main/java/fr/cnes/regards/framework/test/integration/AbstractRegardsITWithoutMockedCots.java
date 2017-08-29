/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.test.integration;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * Base class to realize integration tests using JWT and MockMvc. Should hold all the configurations to be considred by
 * any of its children.
 *
 * @author svissier
 * @author Sébastien Binda
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@ContextConfiguration(classes = { DefaultTestFeignConfiguration.class })
@AutoConfigureMockMvc
@ActiveProfiles({ "default", "test" })
public abstract class AbstractRegardsITWithoutMockedCots {

    /**
     * Default tenant configured in application properties
     */
    protected static final String DEFAULT_TENANT = "PROJECT";

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
     * JSON path for content id in responses
     */
    protected static final String JSON_ID = "$.content.id";

    /**
     * JSON path root in responses
     */
    protected static final String JSON_PATH_ROOT = "$";

    /**
     * JSON path $.* in responses
     */
    protected static final String JSON_PATH_STAR = "$.*";

    /**
     * URL Path separator
     */
    protected static final String URL_PATH_SEPARATOR = "/";

    /**
     * Contract repository
     */
    protected static final Path CONTRACT_REPOSITORY = Paths.get("src", "test", "resources", "contracts");

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

    /**
     * Global {@link GsonBuilder}
     */
    @Autowired
    protected GsonBuilder gsonBuilder;

    /**
     * Mock for MVC testing
     */
    @Autowired
    protected MockMvc mvc;

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

    // File upload

    protected ResultActions performFileUpload(final String pUrlTemplate, final String pAuthenticationToken,
            final Path pFilePath, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final Object... pUrlVariables) {
        final MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(pAuthenticationToken, pFilePath,
                                                                                        pUrlTemplate, pUrlVariables);
        return performRequest(requestBuilder, pMatchers, pErrorMessage);
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

    protected ResultActions performDefaultFileUploadPost(final String pUrlTemplate, final Path pFilePath,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final Object... pUrlVariables) {
        return performDefaultFileUpload(RequestMethod.POST, pUrlTemplate, pFilePath, pMatchers, pErrorMessage,
                                        pUrlVariables);
    }

    protected ResultActions performDefaultFileUploadPost(final String pUrlTemplate,
            final List<MockMultipartFile> pFileList, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final Object... pUrlVariables) {
        return performDefaultFileUpload(RequestMethod.POST, pUrlTemplate, pFileList, pMatchers, pErrorMessage,
                                        pUrlVariables);
    }

    protected ResultActions performDefaultFileUpload(RequestMethod verb, final String pUrlTemplate, final Path filePath,
            final List<ResultMatcher> pMatchers, final String pErrorMessage, final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, verb);
        final MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(jwt, filePath, pUrlTemplate,
                                                                                        pUrlVariables);
        return performRequest(requestBuilder, pMatchers, pErrorMessage);
    }

    protected ResultActions performDefaultFileUpload(RequestMethod verb, final String pUrlTemplate,
            final List<MockMultipartFile> pFileList, final List<ResultMatcher> pMatchers, final String pErrorMessage,
            final Object... pUrlVariables) {
        final String jwt = manageDefaultSecurity(pUrlTemplate, verb);
        final MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(jwt, pFileList, pUrlTemplate,
                                                                                        pUrlVariables);
        return performRequest(requestBuilder, pMatchers, pErrorMessage);
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
            throw new AssertionError(pErrorMessage, e);
        }
    }

    protected MockHttpServletRequestBuilder getRequestBuilder(final String pAuthToken, final HttpMethod pHttpMethod,
            final String pUrlTemplate, final Object... pUrlVars) {

        final MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.request(pHttpMethod, pUrlTemplate,
                                                                                            pUrlVars);
        addSecurityHeader(requestBuilder, pAuthToken);

        requestBuilder.header(HttpConstants.CONTENT_TYPE, "application/json");
        requestBuilder.header(HttpConstants.ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE);

        return requestBuilder;
    }

    /**
     * Build a multi-part request builder based on file {@link Path}
     *
     * @param pAuthToken
     *            authorization token
     * @param pFilePath
     *            {@link Path}
     * @param pUrlTemplate
     *            URL template
     * @param pUrlVars
     *            URL vars
     * @return {@link MockMultipartHttpServletRequestBuilder}
     */
    protected MockMultipartHttpServletRequestBuilder getMultipartRequestBuilder(final String pAuthToken,
            final Path pFilePath, final String pUrlTemplate, final Object... pUrlVars) {

        try {
            final MockMultipartFile file = new MockMultipartFile("file", Files.newInputStream(pFilePath));
            final List<MockMultipartFile> fileList = new ArrayList<>(1);
            fileList.add(file);
            return getMultipartRequestBuilder(pAuthToken, fileList, pUrlTemplate, pUrlVars);
        } catch (final IOException e) {
            final String message = String.format("Cannot create input stream for file %s", pFilePath.toString());
            getLogger().error(message, e);
            throw new AssertionError(message, e);
        }
    }

    protected MockMultipartHttpServletRequestBuilder getMultipartRequestBuilder(final String pAuthToken,
            final List<MockMultipartFile> pFiles, final String pUrlTemplate, final Object... pUrlVars) {

        final MockMultipartHttpServletRequestBuilder multipartRequestBuilder = MockMvcRequestBuilders
                .fileUpload(pUrlTemplate, pUrlVars);
        for (final MockMultipartFile file : pFiles) {
            multipartRequestBuilder.file(file);
        }
        addSecurityHeader(multipartRequestBuilder, pAuthToken);
        return multipartRequestBuilder;
    }

    protected void addSecurityHeader(final MockHttpServletRequestBuilder pRequestBuilder, final String pAuthToken) {
        pRequestBuilder.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + pAuthToken);
    }

    /**
     * Extract payload data from response optionally checking media type
     *
     * @param pResultActions
     *            results
     * @return payload data
     */
    protected String payload(final ResultActions pResultActions) {

        Assert.assertNotNull(pResultActions);
        final MockHttpServletResponse response = pResultActions.andReturn().getResponse();
        try {
            return response.getContentAsString();
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            getLogger().error("Cannot parse payload data");
            throw new AssertionError(e);
        }
    }

    /**
     * Check response media type
     *
     * @param pResultActions
     *            results
     * @param pMediaType
     *            {@link MediaType}
     */
    protected void assertMediaType(final ResultActions pResultActions, final MediaType pMediaType) {
        Assert.assertNotNull(pResultActions);
        Assert.assertNotNull(pMediaType);
        final MockHttpServletResponse response = pResultActions.andReturn().getResponse();
        final MediaType current = MediaType.parseMediaType(response.getContentType());
        Assert.assertEquals(pMediaType, current);
    }

    // CHECKSTYLE:OFF
    protected String gson(final Object pObject) {
        if (pObject instanceof String) {
            return (String) pObject;
        }
        final Gson gson = gsonBuilder.create();
        return gson.toJson(pObject);
    }
    // CHECKSTYLE:ON

    /**
     * Generate token for default tenant
     *
     * @param pName
     *            user name
     * @param pRole
     *            user role
     * @return JWT
     */
    protected String generateToken(final String pName, final String pRole) {
        return jwtService.generateToken(DEFAULT_TENANT, pName, pRole);
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
        authService.setAuthorities(DEFAULT_TENANT, pUrlPath, "osef", pMethod, pRoleNames);
    }

    /**
     * Helper method to manage security with :
     * <ul>
     * <li>an email representing the user</li>
     * <li>the user role </li>
     * </ul>
     * The helper generates a JWT using its configuration and grants access to the endpoint for the specified role
     * role.
     *
     * @param pUrlPath
     *            target endpoint
     * @param pMethod
     *            target HTTP method
     * @return security token to authenticate user
     */
    protected String manageSecurity(final String pUrlPath, final RequestMethod pMethod, String email, String roleName) {

        String path = pUrlPath;
        if (pUrlPath.contains("?")) {
            path = path.substring(0, pUrlPath.indexOf('?'));
        }
        final String jwt = generateToken(email, roleName);
        setAuthorities(path, pMethod, roleName);
        return jwt;
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
        return manageSecurity(pUrlPath, pMethod, DEFAULT_USER_EMAIL, getDefaultRole());
    }

    /**
     * Helper method to manage default security with :
     * <ul>
     * <li>a specific user</li>
     * <li>a default role</li>
     * </ul>
     * The helper generates a JWT using its default configuration and grants access to the endpoint for the default
     * role.
     *
     * @param userEmail specific user
     * @param pUrlPath
     *            target endpoint
     * @param pMethod
     *            target HTTP method
     * @return security token to authenticate user
     */
    protected String manageDefaultSecurity(String userEmail, final String pUrlPath, final RequestMethod pMethod) {
        return manageSecurity(pUrlPath, pMethod, userEmail, getDefaultRole());
    }

    protected static MultiValueMap<String, String> buildRequestParams() {
        return new LinkedMultiValueMap<String, String>();
    }

    protected String getDefaultRole() {
        return DEFAULT_ROLE;
    }

    /**
     * Utility method to read an external JSON file and get it as a string to perform a HTTP request
     *
     * @param pJSonFileName
     *            JSON file contract in {@link AbstractRegardsIT#CONTRACT_REPOSITORY}
     * @return JSON as string
     */
    protected String readJsonContract(String pJSonFileName) {

        Path contract = CONTRACT_REPOSITORY.resolve(pJSonFileName);

        if (Files.exists(contract)) {
            try (JsonReader reader = new JsonReader(new FileReader(contract.toFile()))) {
                JsonElement el = Streams.parse(reader);
                return el.toString();
            } catch (IOException e) {
                String message = "Cannot read JSON contract";
                getLogger().error(message, e);
                throw new AssertionError(message, e);
            }
        } else {
            String message = String.format("File does not exist : %s", pJSonFileName);
            getLogger().error(message);
            throw new AssertionError(message);
        }
    }
}
