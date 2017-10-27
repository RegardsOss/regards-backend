package fr.cnes.regards.framework.test.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;

/**
 * Allow to customize the request done thanks to {@link MockMvc}.
 * Methods "performXX" are considered terminal and so applies coherence controls on the customizations.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class RequestBuilderCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBuilderCustomizer.class);

    private static final HttpHeaders DEFAULT_HEADERS = new HttpHeaders();

    private final HttpHeaders headers = new HttpHeaders();

    private final RequestParamBuilder requestParamBuilder = RequestParamBuilder.build();

    private final List<Snippet> documentationSnippets = Lists.newArrayList();

    private final List<ResultMatcher> expectations = Lists.newArrayList();

    private boolean skipDocumentation = false;

    private GsonBuilder gsonBuilder;

    {
        //lets initiate the default headers!
        DEFAULT_HEADERS.add(HttpConstants.CONTENT_TYPE, "application/json");
        DEFAULT_HEADERS.add(HttpConstants.ACCEPT, "application/json");
    }

    public RequestBuilderCustomizer(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
    }

    public RequestBuilderCustomizer skipDocumentation() {
        skipDocumentation = true;
        return this;
    }

    /**
     *
     * @param urlTemplate
     * @param errorMsg
     * @param urlVariables
     * @return
     */
    protected ResultActions performGet(MockMvc mvc, String urlTemplate, String authToken, String errorMsg,
            Object... urlVariables) {
        return performRequest(mvc, getRequestBuilder(authToken, HttpMethod.GET, urlTemplate, urlVariables), errorMsg);
    }

    /**
     *
     * @param urlTemplate
     * @param errorMsg
     * @param urlVariables
     * @return
     */
    protected ResultActions performDelete(MockMvc mvc, String urlTemplate, String authToken, String errorMsg,
            Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.DELETE, urlTemplate, urlVariables),
                              errorMsg);
    }

    protected ResultActions performPost(MockMvc mvc, String urlTemplate, String authToken, Object content,
            String errorMsg, Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.POST, content, urlTemplate, urlVariables),
                              errorMsg);
    }

    protected ResultActions performPut(MockMvc mvc, String urlTemplate, String authToken, Object content,
            String errorMsg, Object... urlVariables) {
        return performRequest(mvc,
                              getRequestBuilder(authToken, HttpMethod.PUT, content, urlTemplate, urlVariables),
                              errorMsg);
    }

    protected ResultActions performFileUpload(MockMvc mvc, String urlTemplate, String authToken,
            List<MockMultipartFile> files, String errorMsg, Object... urlVariables) {
        return performRequest(mvc, getMultipartRequestBuilder(authToken, files, urlTemplate, urlVariables), errorMsg);
    }

    protected ResultActions performFileUpload(MockMvc mvc, String urlTemplate, String authToken, Path filePath,
            String errorMsg, Object... urlVariables) {
        return performRequest(mvc,
                              getMultipartRequestBuilder(authToken, filePath, urlTemplate, urlVariables),
                              errorMsg);
    }

    /**
     *
     * @param authToken authorization token
     * @param method should be one of: {@link HttpMethod#POST} or {@link HttpMethod#PUT}
     * @param content body of the request
     * @param urlTemplate
     * @param urlVariables
     * @return
     */
    private MockHttpServletRequestBuilder getRequestBuilder(String authToken, HttpMethod method, Object content,
            String urlTemplate, Object... urlVariables) {
        MockHttpServletRequestBuilder requestBuilder = getRequestBuilder(authToken, method, urlTemplate, urlVariables);
        String jsonContent = gson(content);
        requestBuilder.content(jsonContent);
        return requestBuilder;
    }

    protected String gson(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        Gson gson = gsonBuilder.create();
        return gson.toJson(object);
    }

    public RequestParamBuilder customizeRequestParam() {
        return requestParamBuilder;
    }

    public HttpHeaders customizeHeaders() {
        return headers;
    }

    public void addExpectations(List<ResultMatcher> matchers) {
        expectations.addAll(matchers);
    }

    public void addExpectation(ResultMatcher matcher) {
        expectations.add(matcher);
    }

    public void addDocumentationSnippet(Snippet snippet) {
        documentationSnippets.add(snippet);
    }

    /**
     * @param mvc {@link MockMvc} to use for the request
     * @param requestBuilder request builder
     * @param errorMsg message if error occurs
     * @return result
     */
    private ResultActions performRequest(MockMvc mvc, MockHttpServletRequestBuilder requestBuilder, String errorMsg) {
        Assert.assertTrue("At least one expectation is required", expectations.size() > 0);
        try {
            Map<String, Object> queryParams = Maps.newHashMap();
            List<ParameterDescriptor> queryParamDescriptors = Lists.newArrayList();
            if (requestParamBuilder != null) {
                // lets create the attributes and description for the documentation snippet
                requestBuilder.params(requestParamBuilder.getParameters());
                for (Map.Entry<String, List<String>> entry : requestParamBuilder.getParameters().entrySet()) {
                    if (entry.getValue().size() == 1) {
                        queryParams.put(entry.getKey(), entry.getValue().get(0));
                    } else {
                        queryParams.put(entry.getKey(), entry.getValue());
                    }
                    queryParamDescriptors.add(RequestDocumentation.parameterWithName(entry.getKey()).description(""));
                }
            }
            ResultActions request = mvc.perform(requestBuilder);
            for (ResultMatcher matcher : expectations) {
                request = request.andExpect(matcher);
            }
            if (!skipDocumentation) {
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
            }
            return request;
            // CHECKSTYLE:OFF
        } catch (Exception e) {
            // CHECKSTYLE:ON
            LOGGER.error(errorMsg, e);
            throw new AssertionError(errorMsg, e);
        }
    }

    /**
     * Build a multi-part request builder based on file {@link Path}
     * @param authToken authorization token
     * @param filePath {@link Path}
     * @param urlTemplate URL template
     * @param urlVars URL vars
     * @return {@link MockMultipartHttpServletRequestBuilder}
     */
    protected MockMultipartHttpServletRequestBuilder getMultipartRequestBuilder(String authToken, Path filePath,
            String urlTemplate, Object... urlVars) {

        try {
            MockMultipartFile file = new MockMultipartFile("file", Files.newInputStream(filePath));
            List<MockMultipartFile> fileList = new ArrayList<>(1);
            fileList.add(file);
            return getMultipartRequestBuilder(authToken, fileList, urlTemplate, urlVars);
        } catch (IOException e) {
            String message = String.format("Cannot create input stream for file %s", filePath.toString());
            LOGGER.error(message, e);
            throw new AssertionError(message, e);
        }
    }

    /**
     * Build a multi-part request builder based on file {@link Path}
     * @param authToken authorization token
     * @param files {@link MockMultipartFile}s
     * @param urlTemplate URL template
     * @param urlVars URL vars
     * @return {@link MockMultipartHttpServletRequestBuilder}
     */
    protected MockMultipartHttpServletRequestBuilder getMultipartRequestBuilder(String authToken,
            List<MockMultipartFile> files, String urlTemplate, Object... urlVars) {
        // we check with HttpMethod POST because fileUpload method generates a POST request.
        checkCustomizationCoherence(HttpMethod.POST);

        MockMultipartHttpServletRequestBuilder multipartRequestBuilder = RestDocumentationRequestBuilders
                .fileUpload(urlTemplate, urlVars);
        for (MockMultipartFile file : files) {
            multipartRequestBuilder.file(file);
        }
        addSecurityHeader(multipartRequestBuilder, authToken);
        multipartRequestBuilder.headers(getHeaders());
        return multipartRequestBuilder;
    }

    /**
     * @return {@link MockHttpServletRequestBuilder} customized with RequestBuilderCustomizer#headers or default ones if none has been specified
     */
    protected MockHttpServletRequestBuilder getRequestBuilder(String authToken, HttpMethod httpMethod,
            String urlTemplate, Object... urlVars) {
        checkCustomizationCoherence(httpMethod);
        MockHttpServletRequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .request(httpMethod, urlTemplate, urlVars);
        addSecurityHeader(requestBuilder, authToken);

        requestBuilder.headers(getHeaders());

        return requestBuilder;
    }

    protected void checkCustomizationCoherence(HttpMethod httpMethod) {
        // constaints are only on DELETE, PUT and POST, for now, as they cannot have request parameters
        switch (httpMethod) {
            case DELETE:
            case PUT:
            case POST:
                if (!requestParamBuilder.getParameters().isEmpty()) {
                    throw new IllegalStateException(String.format("Method %s cannot have request parameters"));
                }
                break;
            default:
                break;
        }
    }

    protected void addSecurityHeader(MockHttpServletRequestBuilder requestBuilder, String authToken) {
        requestBuilder.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + authToken);
    }

    /**
     * Contains logic on which headers should be used for a request.
     * @return default headers if no header customization has been done. Customized headers otherwise.
     */
    protected HttpHeaders getHeaders() {
        if (headers.isEmpty()) {
            return DEFAULT_HEADERS;
        } else {
            return headers;
        }
    }

}
