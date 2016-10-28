/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import org.springframework.util.Assert;

/**
 *
 * Method parameter definition
 *
 * @param <T>
 *            parameter type
 * @author msordi
 *
 */
public class MethodParam<T> {

    /**
     * Parameter type
     */
    private final Class<T> parameterType;

    /**
     * Parameter value
     */
    private final T value;

    public MethodParam(final Class<T> pParameterType, final T pValue) {
        Assert.notNull(pParameterType);
        Assert.notNull(pValue);
        this.parameterType = pParameterType;
        this.value = pValue;
    }

    public T getValue() {
        return value;
    }

    public Class<T> getParameterType() {
        return parameterType;
    }
}
