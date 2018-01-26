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
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;

/**
 * Base class to realize integration tests using JWT and MockMvc and mocked Cots. Should hold all the configurations to
 * be considered by any of its children.
 *
 * @author svissier
 * @author Sébastien Binda
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public abstract class AbstractRegardsIT extends AbstractRegardsServiceIT {

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
     * Authorization service method
     */
    @Autowired
    protected MethodAuthorizationService authService;

    /**
     * Mock for MVC testing
     */
    @Autowired
    protected MockMvc mvc;

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performPost(String, String, Object, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performPostWithContentType(String urlTemplate, String authToken, Object content,
            String contentType, List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        return performPost(urlTemplate, authToken, content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performPut(String, String, Object, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performPutWithContentType(String urlTemplate, String authToken, Object content,
            String contentType, List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        return performPut(urlTemplate, authToken, content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    // Automatic default security management methods

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performGet(String, String, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultGet(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            RequestParamBuilder requestParams, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.GET);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        for (Map.Entry<String, List<String>> requestParam : requestParams.getParameters().entrySet()) {
            requestBuilderCustomizer.customizeRequestParam()
                    .param(requestParam.getKey(),
                           requestParam.getValue().toArray(new String[requestParam.getValue().size()]));
        }
        return performGet(urlTemplate, jwt, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform GET request with the security automatically handled
     */
    protected ResultActions performDefaultGet(String urlTemplate, RequestBuilderCustomizer requestBuilderCustomizer,
            String errorMsg, Object... urlVariables) {
        return performGet(urlTemplate, manageDefaultSecurity(urlTemplate, RequestMethod.GET), requestBuilderCustomizer,
                          errorMsg, urlVariables);
    }

    /**
     * Allows to perform GET request without the security automatically handled
     */
    protected ResultActions performGet(String urlTemplate, String authToken,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performGet(mvc, urlTemplate, authToken, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performGet(String, String, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultGet(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.GET);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        return performGet(urlTemplate, jwt, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultGet(String, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultGet(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            HttpHeaders headers, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.GET);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            requestBuilderCustomizer.customizeHeaders().put(header.getKey(), header.getValue());
        }
        return performGet(urlTemplate, jwt, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultPost(String, Object, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultPost(String urlTemplate, Object content, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        return performPost(urlTemplate, jwt, content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform POST request with the security automatically handled
     */
    protected ResultActions performDefaultPost(String urlTemplate, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return performPost(urlTemplate, manageDefaultSecurity(urlTemplate, RequestMethod.POST), content,
                           requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform POST request without the security automatically handled
     */
    protected ResultActions performPost(String urlTemplate, String token, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performPost(mvc, urlTemplate, token, content, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultPut(String, Object, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultPut(String urlTemplate, Object content, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.PUT);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        return performPut(urlTemplate, jwt, content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform PUT request with the security automatically handled
     */
    protected ResultActions performDefaultPut(String urlTemplate, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return performPut(urlTemplate, manageDefaultSecurity(urlTemplate, RequestMethod.PUT), content,
                          requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform PUT request without the security automatically handled
     */
    protected ResultActions performPut(String urlTemplate, String token, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performPut(mvc, urlTemplate, token, content, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultPost(String, Object, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultPostWithContentType(String urlTemplate, Object content, String contentType,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        return performPostWithContentType(urlTemplate, jwt, content, contentType, matchers, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultPut(String, Object, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultPutWithContentType(String urlTemplate, Object content, String contentType,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.PUT);
        return performPutWithContentType(urlTemplate, jwt, content, contentType, matchers, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultDelete(String, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultDelete(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.DELETE);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        return performDelete(urlTemplate, jwt, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform DELETE request with the security automatically handled
     */
    protected ResultActions performDefaultDelete(String urlTemplate, RequestBuilderCustomizer requestBuilderCustomizer,
            String errorMsg, Object... urlVariables) {
        return performDelete(urlTemplate, manageDefaultSecurity(urlTemplate, RequestMethod.DELETE),
                             requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform DELETE request without the security automatically handled
     */
    protected ResultActions performDelete(String urlTemplate, String authToken,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performDelete(mvc, urlTemplate, authToken, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultFileUpload(String, Path, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultFileUpload(String urlTemplate, Path pFilePath, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        return performFileUpload(urlTemplate, jwt, pFilePath, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Multipart request uses "file" as part name at the moment
     */
    protected ResultActions performDefaultFileUpload(String urlTemplate, Path pFilePath,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        return performFileUpload(urlTemplate, jwt, pFilePath, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * @deprecated Deprecated in favor of
     * {@link AbstractRegardsIT#performDefaultFileUpload(String, List, RequestBuilderCustomizer, String, Object...)}
     */
    @Deprecated
    protected ResultActions performDefaultFileUpload(String urlTemplate, List<MockMultipartFile> pFileList,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectations(matchers);
        return performFileUpload(urlTemplate, jwt, pFileList, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Multipart request uses "file" as part name at the moment
     */
    protected ResultActions performDefaultFileUpload(String urlTemplate, List<MockMultipartFile> pFileList,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        return performFileUpload(urlTemplate, jwt, pFileList, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Multipart request uses "file" as part name at the moment
     */
    protected ResultActions performFileUpload(String urlTemplate, String jwt, List<MockMultipartFile> pFileList,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performFileUpload(mvc, urlTemplate, jwt, pFileList, errorMsg, urlVariables);
    }

    /**
     * Multipart request uses "file" as part name at the moment
     */
    protected ResultActions performFileUpload(String urlTemplate, String jwt, Path pFilePath,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performFileUpload(mvc, urlTemplate, jwt, pFilePath, errorMsg, urlVariables);
    }

    /**
     * Extract payload data from response optionally checking media type
     * @param pResultActions results
     * @return payload data
     */
    protected String payload(ResultActions pResultActions) {

        Assert.assertNotNull(pResultActions);
        MockHttpServletResponse response = pResultActions.andReturn().getResponse();
        try {
            return response.getContentAsString();
        } catch (UnsupportedEncodingException | RuntimeException e) {
            getLogger().error("Cannot parse payload data");
            throw new AssertionError(e);
        }
    }

    /**
     * Check response media type
     * @param pResultActions results
     * @param pMediaType {@link MediaType}
     */
    protected void assertMediaType(ResultActions pResultActions, MediaType pMediaType) {
        Assert.assertNotNull(pResultActions);
        Assert.assertNotNull(pMediaType);
        MockHttpServletResponse response = pResultActions.andReturn().getResponse();
        MediaType current = MediaType.parseMediaType(response.getContentType());
        Assert.assertEquals(pMediaType, current);
    }

    /**
     * Allow to get a request builder customizer to make a request to the server
     * @return a new instance of {@link RequestBuilderCustomizer}
     */
    protected RequestBuilderCustomizer getNewRequestBuilderCustomizer() {
        return new RequestBuilderCustomizer(gsonBuilder);
    }
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF

    /**
     * Set authorities for default tenant
     * @param pUrlPath endpoint
     * @param pMethod HTTP method
     * @param pRoleNames list of roles
     */
    protected void setAuthorities(String pUrlPath, RequestMethod pMethod, String... pRoleNames) {
        authService.setAuthorities(DEFAULT_TENANT, pUrlPath, "osef", pMethod, pRoleNames);
    }

    /**
     * Helper method to manage security with :
     * <ul>
     * <li>an email representing the user</li>
     * <li>the user role</li>
     * </ul>
     * The helper generates a JWT using its configuration and grants access to the endpoint for the specified role
     * role.
     * @param pUrlPath target endpoint
     * @param pMethod target HTTP method
     * @return security token to authenticate user
     */
    protected String manageSecurity(String pUrlPath, RequestMethod pMethod, String email, String roleName) {

        String path = pUrlPath;
        if (pUrlPath.contains("?")) {
            path = path.substring(0, pUrlPath.indexOf('?'));
        }
        String jwt = generateToken(email, roleName);
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
     * @param pUrlPath target endpoint
     * @param pMethod target HTTP method
     * @return security token to authenticate user
     */
    protected String manageDefaultSecurity(String pUrlPath, RequestMethod pMethod) {
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
     * @param userEmail specific user
     * @param pUrlPath target endpoint
     * @param pMethod target HTTP method
     * @return security token to authenticate user
     */
    protected String manageDefaultSecurity(String userEmail, String pUrlPath, RequestMethod pMethod) {
        return manageSecurity(pUrlPath, pMethod, userEmail, getDefaultRole());
    }

    /**
     * Utility method to read an external JSON file and get it as a string to perform a HTTP request
     * @param pJSonFileName JSON file contract in {@link AbstractRegardsIT#CONTRACT_REPOSITORY}
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

    /**
     * Allows to jsonify an object thanks to GSON
     * @param object
     * @return jsonified object
     */
    protected String gson(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        Gson gson = gsonBuilder.create();
        return gson.toJson(object);
    }
}
