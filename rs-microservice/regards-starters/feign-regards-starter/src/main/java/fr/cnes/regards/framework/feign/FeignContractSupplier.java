/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestParamParameterProcessor;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;

import feign.Contract;

/**
 * Supplies the custom SpringMvcContract to use with Feign.
 *
 * @author Xavier-Alexandre Brochard
 */
public class FeignContractSupplier implements Supplier<Contract> {

    /* (non-Javadoc)
     * @see java.util.function.Supplier#get()
     */
    @Override
    public Contract get() {
        return new SpringMvcContract(getCustomAnnotatedArgumentsProcessors());
    }

    /**
     * Customize the default AnnotatedArgumentsProcessors in order to use
     * our {@link CustomRequestParamParameterProcessor} instead of the {@link RequestParamParameterProcessor}
     *
     * @return the list of processors
     */
    private List<AnnotatedParameterProcessor> getCustomAnnotatedArgumentsProcessors() {
        List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

        annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
        annotatedArgumentResolvers.add(new CustomRequestParamParameterProcessor());
        annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

        return annotatedArgumentResolvers;
    }

}
