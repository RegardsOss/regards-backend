/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.util.Assert;

/**
 *
 * Resource utilities
 *
 * @author msordi
 *
 */
public final class ResourceUtils {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtils.class);

    private ResourceUtils() {
    }

    /**
     * Convert object to resource
     *
     * @param <T>
     *            element to convert
     * @param pObject
     *            object
     * @return {@link Resource}
     */
    public static <T> Resource<T> toResource(T pObject) {
        Assert.notNull(pObject);
        return new Resource<T>(pObject);
    }

    /**
     * Enable link support
     *
     * @return {@link ResourceSupport}
     */
    public static ResourceSupport getResourceSupport() {
        return new ResourceSupport();
    }

    /**
     * Add a link to a resource for a single method
     *
     * @param pResource
     *            resource to manage
     * @param pController
     *            controller
     * @param pMethodName
     *            method name
     * @param pRel
     *            rel name
     * @param pMethodParams
     *            method parameters
     */
    public static void addLink(ResourceSupport pResource, Class<?> pController, String pMethodName, String pRel,
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
            final Method method = ResourceUtils.getMethod(pController, pMethodName, parameterTypes);
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
    private static Method getMethod(Class<?> pController, String pMethodName, Class<?>... pParameterTypes)
            throws MethodException {
        try {
            return pController.getMethod(pMethodName, pParameterTypes);
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
}
