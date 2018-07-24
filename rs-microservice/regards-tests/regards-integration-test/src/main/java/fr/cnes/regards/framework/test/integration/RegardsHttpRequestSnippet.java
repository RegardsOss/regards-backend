package fr.cnes.regards.framework.test.integration;

import java.util.Map;

import org.springframework.restdocs.generate.RestDocumentationGenerator;
import org.springframework.restdocs.http.HttpRequestSnippet;
import org.springframework.restdocs.operation.Operation;
import org.springframework.util.Assert;

/**
 * REGARDS customization of {@link HttpRequestSnippet}.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Marc Sordi
 */
public class RegardsHttpRequestSnippet extends HttpRequestSnippet {

    private static final String REQUEST_BODY = "requestBody";

    private static final String URL_TEMPLATE = "urlTemplate";

    @Override
    public Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = super.createModel(operation);
        extractUrlTemplate(model, operation);
        cleanRequestBody(model);
        return model;
    }

    private void extractUrlTemplate(Map<String, Object> model, Operation operation) {
        String urlTemplate = (String) operation.getAttributes()
                .get(RestDocumentationGenerator.ATTRIBUTE_NAME_URL_TEMPLATE);
        Assert.notNull(urlTemplate, "urlTemplate not found. If you are using MockMvc did "
                + "you use RestDocumentationRequestBuilders to build the request?");
        model.put(URL_TEMPLATE, urlTemplate);
    }

    /**
     * If request body not useful, remove it for good Mustache template rendering
     */
    private void cleanRequestBody(Map<String, Object> model) {

        if (model.containsKey(REQUEST_BODY)) {
            Object body = model.get(REQUEST_BODY);
            if (body == null) {
                model.remove(REQUEST_BODY);
            } else {
                if (body instanceof String) {
                    String stringBody = (String) body;
                    if (stringBody.isEmpty()) {
                        model.remove(REQUEST_BODY);
                    }
                }
            }
        }
    }
}
