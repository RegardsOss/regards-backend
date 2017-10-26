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
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;

/**
 * Base class to realize integration tests using JWT and MockMvc and mocked Cots. Should hold all the configurations to
 * be considered by any of its children.
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

    protected static MultiValueMap<String, String> buildRequestParams() {
        return new LinkedMultiValueMap<String, String>();
    }

    protected ResultActions performGet(String urlTemplate, String authToken, List<ResultMatcher> matchers,
            String errorMsg, HttpHeaders headers, Object... urlVariables) {
        return performRequest(authToken, HttpMethod.GET, urlTemplate, matchers, errorMsg, headers, urlVariables);
    }

    protected ResultActions performGet(String urlTemplate, String authToken, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        return performRequest(authToken, HttpMethod.GET, urlTemplate, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performGet(String urlTemplate, String authToken, List<ResultMatcher> matchers,
            String errorMsg, RequestParamBuilder requestParams, Object... urlVariables) {
        return performRequest(authToken, HttpMethod.GET, urlTemplate, matchers, errorMsg, requestParams, urlVariables);
    }

    protected ResultActions performPost(String urlTemplate, String authToken, Object content,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        return performRequest(authToken, HttpMethod.POST, urlTemplate, content, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performPut(String urlTemplate, String authToken, Object content,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        return performRequest(authToken, HttpMethod.PUT, urlTemplate, content, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performPostWithContentType(String urlTemplate, String authToken, Object content,
            String contentType, List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        return performRequestWithContentType(authToken,
                                             HttpMethod.POST,
                                             urlTemplate,
                                             content,
                                             contentType,
                                             matchers,
                                             errorMsg,
                                             urlVariables);
    }

    protected ResultActions performPutWithContentType(String urlTemplate, String authToken, Object content,
            String contentType, List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        return performRequestWithContentType(authToken,
                                             HttpMethod.PUT,
                                             urlTemplate,
                                             content,
                                             contentType,
                                             matchers,
                                             errorMsg,
                                             urlVariables);
    }

    // File upload

    protected ResultActions performDelete(String urlTemplate, String authToken, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        return performRequest(authToken, HttpMethod.DELETE, urlTemplate, matchers, errorMsg, urlVariables);
    }

    // Automatic default security management methods

    protected ResultActions performFileUpload(String urlTemplate, String authToken, Path pFilePath,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(authToken,
                                                                                  pFilePath,
                                                                                  urlTemplate,
                                                                                  urlVariables);
        return performRequest(requestBuilder, matchers, errorMsg, null, documentationSnippets);
    }

    protected ResultActions performDefaultGet(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            RequestParamBuilder requestParams, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.GET);
        return performGet(urlTemplate, jwt, matchers, errorMsg, requestParams, urlVariables);
    }

    protected ResultActions performDefaultGet(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.GET);
        return performGet(urlTemplate, jwt, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultGet(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            HttpHeaders headers, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.GET);
        return performGet(urlTemplate, jwt, matchers, errorMsg, headers, urlVariables);
    }

    protected ResultActions performDefaultPost(String urlTemplate, Object content, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        return performPost(urlTemplate, jwt, content, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultPut(String urlTemplate, Object content, List<ResultMatcher> matchers,
            String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.PUT);
        return performPut(urlTemplate, jwt, content, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultPostWithContentType(String urlTemplate, Object content, String contentType,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.POST);
        return performPostWithContentType(urlTemplate, jwt, content, contentType, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultPutWithContentType(String urlTemplate, Object content, String contentType,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.PUT);
        return performPutWithContentType(urlTemplate, jwt, content, contentType, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultDelete(String urlTemplate, List<ResultMatcher> matchers, String errorMsg,
            Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, RequestMethod.DELETE);
        return performDelete(urlTemplate, jwt, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultFileUploadPost(String urlTemplate, Path pFilePath,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        return performDefaultFileUpload(RequestMethod.POST, urlTemplate, pFilePath, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultFileUploadPost(String urlTemplate, List<MockMultipartFile> pFileList,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        return performDefaultFileUpload(RequestMethod.POST, urlTemplate, pFileList, matchers, errorMsg, urlVariables);
    }

    protected ResultActions performDefaultFileUpload(RequestMethod verb, String urlTemplate, Path filePath,
            List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, verb);
        MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(jwt,
                                                                                  filePath,
                                                                                  urlTemplate,
                                                                                  urlVariables);
        return performRequest(requestBuilder, matchers, errorMsg, null, documentationSnippets);
    }

    protected ResultActions performDefaultFileUpload(RequestMethod verb, String urlTemplate,
            List<MockMultipartFile> pFileList, List<ResultMatcher> matchers, String errorMsg, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, verb);
        MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(jwt,
                                                                                  pFileList,
                                                                                  urlTemplate,
                                                                                  urlVariables);
        return performRequest(requestBuilder, matchers, errorMsg, null, Lists.emptyList());
    }

    protected ResultActions performDefaultFileUpload(RequestMethod verb, String urlTemplate,
            List<MockMultipartFile> pFileList, List<ResultMatcher> matchers, String errorMsg, List<Snippet> documentationSnippets, Object... urlVariables) {
        String jwt = manageDefaultSecurity(urlTemplate, verb);
        MockHttpServletRequestBuilder requestBuilder = getMultipartRequestBuilder(jwt,
                                                                                  pFileList,
                                                                                  urlTemplate,
                                                                                  urlVariables);
        return performRequest(requestBuilder, matchers, errorMsg, null, documentationSnippets);
    }

    /**
     * Perform a REST request and control expectations
     * @param authToken JWT token
     * @param pHttpMethod HTTP method
     * @param urlTemplate URL template
     * @param content content for {@link HttpMethod#POST} and {@link HttpMethod#PUT} methods
     * @param contentType media type
     * @param matchers expectations
     * @param errorMsg message if error occurs
     * @param urlVariables URL variables
     * @return result
     */
    protected ResultActions performRequestWithContentType(String authToken, HttpMethod pHttpMethod, String urlTemplate,
            Object content, String contentType, List<ResultMatcher> matchers, String errorMsg,
            List<Snippet> documentationSnippets, Object... urlVariables) {

        Assert.assertTrue(HttpMethod.POST.equals(pHttpMethod) || HttpMethod.PUT.equals(pHttpMethod));
        MockHttpServletRequestBuilder requestBuilder = getRequestBuilder(authToken,
                                                                         pHttpMethod,
                                                                         urlTemplate,
                                                                         urlVariables);
        String jsonContent = gson(content);
        requestBuilder.content(jsonContent);
        requestBuilder.contentType(contentType);
        return performRequest(requestBuilder, matchers, errorMsg, null, documentationSnippets);
    }

    protected ResultActions performRequest(String authToken, HttpMethod pHttpMethod, String urlTemplate, Object content,
            List<ResultMatcher> matchers, String errorMsg, List<Snippet> documentationSnippets,
            Object... urlVariables) {
        return performRequestWithContentType(authToken,
                                             pHttpMethod,
                                             urlTemplate,
                                             content,
                                             MediaType.APPLICATION_JSON_VALUE,
                                             matchers,
                                             errorMsg,
                                             documentationSnippets,
                                             urlVariables);
    }



    /**
     * @param pRequestBuilder request builder
     * @param matchers expectations
     * @param errorMsg message if error occurs
     * @param requestParamBuilder
     * @param documentationSnippets
     * @return result
     */
    protected ResultActions performRequest(MockHttpServletRequestBuilder pRequestBuilder, List<ResultMatcher> matchers,
            String errorMsg, RequestParamBuilder requestParamBuilder, List<Snippet> documentationSnippets) {
        try {
            Map<String, Object> queryParams = Maps.newHashMap();
            List<ParameterDescriptor> queryParamDescriptors = Lists.newArrayList();
            if (requestParamBuilder != null) {
                // lets create the attributes and description for the documentation snippet
                pRequestBuilder.params(requestParamBuilder.getParameters());
                for (Map.Entry<String, List<String>> entry : requestParamBuilder.getParameters().entrySet()) {
                    if (entry.getValue().size() == 1) {
                        queryParams.put(entry.getKey(), entry.getValue().get(0));
                    } else {
                        queryParams.put(entry.getKey(), entry.getValue());
                    }
                    queryParamDescriptors.add(RequestDocumentation.parameterWithName(entry.getKey()).description(""));
                }
            }
            ResultActions request = mvc.perform(pRequestBuilder);
            for (ResultMatcher matcher : matchers) {
                request = request.andExpect(matcher);
            }
            request.andDo(MockMvcRestDocumentation.document("{ClassName}/{methodName}",
                                                            preprocessRequest(prettyPrint(),
                                                                              removeHeaders("Authorization",
                                                                                            "Host",
                                                                                            "Content-Length")),
                                                            preprocessResponse(prettyPrint(),
                                                                               removeHeaders("Content-Length")),
                                                            documentationSnippets
                                                                    .toArray(new Snippet[documentationSnippets
                                                                            .size()])));
            return request;
            // CHECKSTYLE:OFF
        } catch (Exception e) {
            // CHECKSTYLE:ON
            getLogger().error(errorMsg, e);
            throw new AssertionError(errorMsg, e);
        }
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
            // CHECKSTYLE:OFF
        } catch (Exception e) {
            // CHECKSTYLE:ON
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
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF
    protected String gson(Object pObject) {
        if (pObject instanceof String) {
            return (String) pObject;
        }
        Gson gson = gsonBuilder.create();
        return gson.toJson(pObject);
    }

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
}
