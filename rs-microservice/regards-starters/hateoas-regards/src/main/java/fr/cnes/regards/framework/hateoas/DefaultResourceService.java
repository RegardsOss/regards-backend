/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Default resource service based on security starter
 *
 * @author msordi
 *
 */
public class DefaultResourceService implements IResourceService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultResourceService.class);

    /**
     * Method authorization service
     */
    @Autowired
    private MethodAuthorizationService authorisationService;

    @Override
    public <T> void addLink(Resource<T> pResource, Class<?> pController, String pMethodName, String pRel,
            MethodParam<?>... pMethodParams) {

        Assert.notNull(pResource);
        Assert.notNull(pController);
        Assert.notNull(pMethodName);
        Assert.notNull(pRel);

        // Prepare method parameters
        Class<?>[] parameterTypes = null;
        Object[] parameterValues = null;
        if (pMethodParams != null) {
            parameterTypes = new Class<?>[pMethodParams.length];
            parameterValues = new String[pMethodParams.length];

            for (int i = 0; i < pMethodParams.length; i++) {
                parameterTypes[i] = pMethodParams[i].getParameterType();
                parameterValues[i] = pMethodParams[i].getValue();
            }
        }

        try {
            final Method method = getMethod(pController, pMethodName, parameterTypes);
            final Link link = ControllerLinkBuilder.linkTo(method, parameterValues).withRel(pRel);
            pResource.add(link);
        } catch (MethodException e) {
            // Do not insert link
        }
    }

    /**
     * Retrieve a method from a class
     *
     * @param pController
     *            class
     * @param pMethodName
     *            method name
     * @param pParameterTypes
     *            parameter types
     * @return associated {@link Method}
     * @throws MethodException
     *             if method cannot be retrieved
     */
    private Method getMethod(Class<?> pController, String pMethodName, Class<?>... pParameterTypes)
            throws MethodException {
        try {
            final Method method = pController.getMethod(pMethodName, pParameterTypes);
            checkAuthorization(method);
            return method;
        } catch (NoSuchMethodException e) {
            final String message = MessageFormat.format("No such method {0} in controller {1}.", pMethodName,
                                                        pController.getCanonicalName());
            LOG.error(message, e);
            throw new MethodException(message);
        } catch (SecurityException e) {
            final String message = MessageFormat.format("Security exception accessing method {0} in controller {1}.",
                                                        pMethodName, pController.getCanonicalName());
            LOG.error(message, e);
            throw new MethodException(message);
        }
    }

    /**
     * Check if method is accessible regarding security authorities
     *
     * @param pMethod
     *            method to check
     * @throws MethodException
     *             if method not authorized
     */
    private void checkAuthorization(Method pMethod) throws MethodException {
        final JWTAuthentication auth = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (!authorisationService.hasAccess(auth, pMethod)) {
            final String message = MessageFormat.format("Unauthorized method {0}", pMethod.getName());
            LOG.error(message);
            throw new MethodException(message);
        }
    }
}
