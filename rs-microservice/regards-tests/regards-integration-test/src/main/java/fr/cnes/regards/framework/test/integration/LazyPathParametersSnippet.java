package fr.cnes.regards.framework.test.integration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.request.AbstractParametersSnippet;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.PathParametersSnippet;
import org.springframework.restdocs.request.RequestDocumentation;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class LazyPathParametersSnippet extends AbstractParametersSnippet {

    private PathParametersSnippet defaultSnippet;

    protected LazyPathParametersSnippet(String snippetName, List<ParameterDescriptor> descriptors,
            Map<String, Object> attributes) {
        super(snippetName, descriptors, attributes, false);
        //Ok so to have all parameters added to the snippet, we need to get all attributes which are parameters and create missing descriptors
        //first lets get the list of all parameter described
        Set<String> describedParameters = getParameterDescriptors().keySet();
        for (String parameterName : attributes.keySet()) {
            if(!describedParameters.contains(parameterName)) {
                getParameterDescriptors().put(parameterName, RequestDocumentation
                        .parameterWithName(parameterName).description(""));
            }
        }
        defaultSnippet = RequestDocumentation.pathParameters(getAttributes(), getParameterDescriptors().values().toArray(new ParameterDescriptor[getParameterDescriptors().values().size()]));
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        return defaultSnippet.cre;
    }

    @Override
    protected Set<String> extractActualParameters(Operation operation) {
        //this is not supposed to be called
        return null;
    }

    @Override
    protected void verificationFailed(Set<String> undocumentedParameters, Set<String> missingParameters) {

    }
}
