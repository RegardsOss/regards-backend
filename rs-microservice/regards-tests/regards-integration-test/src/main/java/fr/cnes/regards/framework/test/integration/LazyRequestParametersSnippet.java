package fr.cnes.regards.framework.test.integration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.request.AbstractParametersSnippet;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;

/**
 * Request parameters snippet that allow to add to the snippet all request parameters, documented or not.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class LazyRequestParametersSnippet extends AbstractParametersSnippet {

    public LazyRequestParametersSnippet(String snippetName, List<ParameterDescriptor> descriptors,
            Map<String, Object> attributes) {
        super(snippetName, descriptors, attributes, false);
        //Ok so to have all parameters added to the snippet, we need to get all attributes which are parameters and create missing descriptors
        //first lets get the list of all parameter described
        Set<String> describedParameters = getParameterDescriptors().keySet();
        for (String parameterName : attributes.keySet()) {
            if(!describedParameters.contains(parameterName)) {
                getParameterDescriptors().put(parameterName, RequestDocumentation.parameterWithName(parameterName).description(""));
            }
        }
    }

    @Override
    protected Set<String> extractActualParameters(Operation operation) {
        return operation.getRequest().getParameters().keySet();
    }

    @Override
    protected void verificationFailed(Set<String> undocumentedParameters, Set<String> missingParameters) {

    }
}
