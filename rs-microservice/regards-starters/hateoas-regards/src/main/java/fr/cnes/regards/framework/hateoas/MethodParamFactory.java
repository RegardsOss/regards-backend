/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

/**
 * Method parameter factory
 *
 * @author msordi
 *
 */
public final class MethodParamFactory {

    private MethodParamFactory() {
    }

    public static <T> MethodParam<T> build(Class<T> pParameterType, T pValue) {
        return new MethodParam<T>(pParameterType, pValue);
    }

}
