/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import static feign.Util.emptyToNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestParamParameterProcessor;
import org.springframework.web.bind.annotation.RequestParam;

import feign.MethodMetadata;

/**
 * Improve the default {@link RequestParamParameterProcessor} in order to teach it how to handle Map<String, String> parameters in REST controllers,
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
public class CustomRequestParamParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<RequestParam> ANNOTATION = RequestParam.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext pContext, Annotation pAnnotation, Method pMethod) {
        String name = ANNOTATION.cast(pAnnotation).value();
        if (emptyToNull(name) != null) {
            pContext.setParameterName(name);

            MethodMetadata data = pContext.getMethodMetadata();
            Collection<String> query = pContext.setTemplateParameter(name, data.template().queries().get(name));
            data.template().query(name, query);
        } else {
            MethodMetadata data = pContext.getMethodMetadata();
            data.queryMapIndex(pContext.getParameterIndex());
        }
        return true;
    }

}
