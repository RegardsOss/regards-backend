/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.lang.reflect.Method;

/**
 * Method security
 *
 * @author msordi
 *
 */
public interface IMethodSecurity {

    Boolean isAuthorized(Method pMethod);
}
