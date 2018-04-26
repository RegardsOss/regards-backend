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
 */
public class RegardsHttpRequestSnippet extends HttpRequestSnippet {

    /**
     * Override path attribute from {@link HttpRequestSnippet#createModel(Operation)}.
     * We do want the template and not the value.
     * @param operation
     * @return
     */
    @Override
    public Map<String, Object> createModel(Operation operation) {
        Map<String, Object> model = super.createModel(operation);
        model.put("path", extractUrlTemplate(operation));
        return model;
    }

    private String extractUrlTemplate(Operation operation) {
        String urlTemplate = (String) operation.getAttributes()
                .get(RestDocumentationGenerator.ATTRIBUTE_NAME_URL_TEMPLATE);
        Assert.notNull(urlTemplate,
                       "urlTemplate not found. If you are using MockMvc did "
                               + "you use RestDocumentationRequestBuilders to build the request?");
        return urlTemplate;
    }
}
