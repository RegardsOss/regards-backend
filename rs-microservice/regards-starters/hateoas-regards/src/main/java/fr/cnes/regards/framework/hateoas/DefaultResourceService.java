/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final MethodAuthorizationService authorisationService;

    /**
     *
     * Constructor
     *
     * @param pAuthorisationService
     *            {@link MethodAuthorizationService} Service for method access authorization management
     * @since 1.0-SNAPSHOT
     */
    public DefaultResourceService(final MethodAuthorizationService pAuthorisationService) {
        super();
        authorisationService = pAuthorisationService;
    }

    @Override
    public <T> void addLink(final Resource<T> pResource, final Class<?> pController, final String pMethodName,
            final String pRel, final MethodParam<?>... pMethodParams) {

        Assert.notNull(pResource);
        Assert.notNull(pController);
        Assert.notNull(pMethodName);
        Assert.notNull(pRel);

        // Prepare method parameters
        Class<?>[] parameterTypes = null;
        List<Object> parameterValues = null;
        if (pMethodParams != null) {
            parameterTypes = new Class<?>[pMethodParams.length];
            parameterValues = new ArrayList<>();
            for (int i = 0; i < pMethodParams.length; i++) {
                parameterTypes[i] = pMethodParams[i].getParameterType();
                if (pMethodParams[i].getValue() != null) {
                    parameterValues.add(pMethodParams[i].getValue());
                }
            }
        }

        try {
            final Method method = getMethod(pController, pMethodName, parameterTypes);
            final Link link;
            if (parameterValues != null) {
                link = buildLink(method, pRel, parameterValues.toArray());
            } else {
                link = buildLink(method, pRel);
            }
            pResource.add(link);
        } catch (final MethodException e) {
            // Do not insert link
        }
    }

    protected Link buildLink(final Method pMethod, final String pRel, final Object... pParameterValues) {
        return ControllerLinkBuilder.linkTo(pMethod, pParameterValues).withRel(pRel);
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
    private Method getMethod(final Class<?> pController, final String pMethodName, final Class<?>... pParameterTypes)
            throws MethodException {
        try {
            final Method method = pController.getMethod(pMethodName, pParameterTypes);
            checkAuthorization(method);
            return method;
        } catch (final NoSuchMethodException e) {
            final String message = MessageFormat.format("No such method {0} in controller {1}.", pMethodName,
                                                        pController.getCanonicalName());
            LOG.error(message, e);
            throw new MethodException(message);
        } catch (final SecurityException e) {
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
    private void checkAuthorization(final Method pMethod) throws MethodException {
        final JWTAuthentication auth = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (!authorisationService.hasAccess(auth, pMethod)) {
            final String message = MessageFormat.format("Unauthorized method {0}", pMethod.getName());
            LOG.debug(message);
            throw new MethodException(message);
        }
    }
}
