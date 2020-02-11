/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;

/**
 * Base class to realize integration tests using JWT and MockMvc and mocked Cots. Should hold all the configurations to
 * be considered by any of its children.
 * @author svissier
 * @author Sébastien Binda
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
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
     * JSON type for API documentation
     */
    protected static final String JSON_ARRAY_TYPE = "Array";

    protected static final String JSON_BOOLEAN_TYPE = "Boolean";

    protected static final String JSON_OBJECT_TYPE = "Object";

    protected static final String JSON_NUMBER_TYPE = "Number";

    protected static final String JSON_NULL_TYPE = "Null";

    protected static final String JSON_STRING_TYPE = "String";

    protected static final String JSON_VARIES_TYPE = "Varies";

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
     * Allows to perform GET request with the security automatically handled
     */
    protected ResultActions performDefaultGet(String urlTemplate, RequestBuilderCustomizer requestBuilderCustomizer,
            String errorMsg, Object... urlVariables) {
        return performGet(urlTemplate, manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.GET,
                                                      getDefaultUserEmail(), getDefaultRole()),
                          requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform GET request without the security automatically handled
     */
    protected ResultActions performGet(String urlTemplate, String authToken,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performGet(mvc, urlTemplate, authToken, errorMsg, urlVariables);
    }

    /**
     * Allows to perform POST request with the security automatically handled
     */
    protected ResultActions performDefaultPost(String urlTemplate, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return performPost(urlTemplate, manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.POST,
                                                       getDefaultUserEmail(), getDefaultRole()),
                           content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform POST request without the security automatically handled
     */
    protected ResultActions performPost(String urlTemplate, String token, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performPost(mvc, urlTemplate, token, content, errorMsg, urlVariables);
    }

    /**
     * Allows to perform PUT request with the security automatically handled
     */
    protected ResultActions performDefaultPut(String urlTemplate, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return performPut(urlTemplate, manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.PUT,
                                                      getDefaultUserEmail(), getDefaultRole()),
                          content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform PUT request without the security automatically handled
     */
    protected ResultActions performPut(String urlTemplate, String token, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performPut(mvc, urlTemplate, token, content, errorMsg, urlVariables);
    }

    /**
     * Allows to perform PATCH request with the security automatically handled
     */
    protected ResultActions performDefaultPatch(String urlTemplate, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return performPatch(urlTemplate,
                            manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.PATCH, getDefaultUserEmail(),
                                           getDefaultRole()),
                            content, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Allows to perform PATCH request without the security automatically handled
     */
    protected ResultActions performPatch(String urlTemplate, String token, Object content,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        return requestBuilderCustomizer.performPatch(mvc, urlTemplate, token, content, errorMsg, urlVariables);
    }

    /**
     * Allows to perform DELETE request with the security automatically handled
     */
    protected ResultActions performDefaultDelete(String urlTemplate, RequestBuilderCustomizer requestBuilderCustomizer,
            String errorMsg, Object... urlVariables) {
        return performDelete(urlTemplate,
                             manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.DELETE,
                                            getDefaultUserEmail(), getDefaultRole()),
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
     * Multipart request uses "file" as part name at the moment
     */
    protected ResultActions performDefaultFileUpload(String urlTemplate, Path pFilePath,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        String jwt = manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.POST, getDefaultUserEmail(),
                                    getDefaultRole());
        return performFileUpload(urlTemplate, jwt, pFilePath, requestBuilderCustomizer, errorMsg, urlVariables);
    }

    /**
     * Multipart request uses "file" as part name at the moment
     */
    protected ResultActions performDefaultFileUpload(String urlTemplate, List<MockMultipartFile> pFileList,
            RequestBuilderCustomizer requestBuilderCustomizer, String errorMsg, Object... urlVariables) {
        String jwt = manageSecurity(getDefaultTenant(), urlTemplate, RequestMethod.POST, getDefaultUserEmail(),
                                    getDefaultRole());
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
     * @return a new RequestBuilderCustomizer
     */
    protected RequestBuilderCustomizer customizer() {
        return new RequestBuilderCustomizer(gsonBuilder);
    }

    /**
     * Set authorities for specified tenant
     * @param tenant related tenant
     * @param urlPath endpoint
     * @param method HTTP method
     * @param roleNames list of roles
     */
    protected void setAuthorities(String tenant, String urlPath, RequestMethod method, String... roleNames) {
        authService.setAuthorities(tenant, urlPath, "osef", method, roleNames);
    }

    /**
     * Use {@link #setAuthorities(String, String, RequestMethod, String...)} instead.
     */
    protected void setAuthorities(String urlPath, RequestMethod method, String... roleNames) {
        authService.setAuthorities(getDefaultTenant(), urlPath, "osef", method, roleNames);
    }

    /**
     * Helper method to manage security with :
     * <ul>
     * <li>an email representing the user</li>
     * <li>the user role</li>
     * </ul>
     * The helper generates a JWT using its configuration and grants access to the endpoint for the specified role
     * role.
     * @param tenant related tenant
     * @param pUrlPath target endpoint
     * @param pMethod target HTTP method
     * @return security token to authenticate user
     */
    protected String manageSecurity(String tenant, String pUrlPath, RequestMethod pMethod, String email,
            String roleName) {

        String path = pUrlPath;
        if (pUrlPath.contains("?")) {
            path = path.substring(0, pUrlPath.indexOf('?'));
        }
        String jwt = generateToken(email, roleName);
        setAuthorities(tenant, path, pMethod, roleName);
        return jwt;
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
