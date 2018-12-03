/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.hateoas;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Converter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Default resource service based on security starter
 * @author msordi
 */
public class DefaultResourceService implements IResourceService {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultResourceService.class);

    /**
     * Security access decision manager (same as security)
     */
    private final AccessDecisionManager accessDecisionManager;

    /**
     * Constructor
     * @param pAccessDecisionManager {@link AccessDecisionManager} for deciding that an access is granted or denied
     */
    public DefaultResourceService(AccessDecisionManager pAccessDecisionManager) {
        super();
        this.accessDecisionManager = pAccessDecisionManager;
    }

    @Override
    public void addLink(ResourceSupport resource, final Class<?> controller, final String methodName, final String rel,
            final MethodParam<?>... methodParams) {

        Assert.notNull(resource, "Resource should not be null");
        Link link = buildLink(controller, methodName, rel, methodParams);
        // May be null if silent error occurs
        if (link != null) {
            resource.add(link);
        }
    }

    @Override
    public Link buildLink(Class<?> controller, String methodName, String rel, MethodParam<?>... methodParams) {
        Assert.notNull(controller, "Controller should not be null");
        Assert.notNull(methodName, "Method name should not be null");
        Assert.notNull(rel, "Relation should not be null");

        // Prepare method parameters
        Class<?>[] parameterTypes = null;
        List<Object> parameterValues = null;
        if (methodParams != null) {
            parameterTypes = new Class<?>[methodParams.length];
            parameterValues = new ArrayList<>();
            for (int i = 0; i < methodParams.length; i++) {
                parameterTypes[i] = methodParams[i].getParameterType();
                if (methodParams[i].getValue() != null) {
                    parameterValues.add(methodParams[i].getValue());
                }
            }
        }

        try {
            final Method method = getMethod(controller, methodName, parameterTypes);
            final Link link;
            if (parameterValues != null) {
                link = buildLink(method, rel, parameterValues.toArray());
            } else {
                link = buildLink(method, rel);
            }
            return link;
        } catch (final MethodException e) {
            // Do not insert link
            LOGGER.trace("HATEOAS link skipped silently due to introspection error or access denied", e);
        }
        return null;
    }

    protected Link buildLink(final Method pMethod, final String pRel, final Object... pParameterValues) {
        return ControllerLinkBuilder.linkTo(pMethod, pParameterValues).withRel(pRel);
    }

    /**
     * Custom way of adding link to a resource handling request params.
     *
     * For example, an endpoint like getSomething(@RequestParam String name)
     * mapped to: "/something"
     * will generate a link like
     * "http://someting?name=myName"
     *
     * BUT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * It cannot handle type conversion, even if you declared the correct Spring {@link Converter}.
     *
     * For example, an endpoint like
     * getSomething(@RequestParam ComplexEntity entity)
     * mapped to: "/something"
     * will generate a conversion error, telling that it could not find the appropriate converter,
     * even if you defined in your classpath a converter implementing Converter<ComplexEntity, String>
     */
    @Override
    public <C> void addLinkWithParams(ResourceSupport resource, final Class<C> controller, final String methodName,
            final String rel, final MethodParam<?>... methodParams) {

        Assert.notNull(resource, "Resource should not be null");
        Link link = buildLinkWithParams(controller, methodName, rel, methodParams);
        // May be null if silent error occurs
        if (link != null) {
            resource.add(link);
        }
    }

    @Override
    public <C> Link buildLinkWithParams(Class<C> controller, String methodName, String rel,
            MethodParam<?>... methodParams) {
        Assert.notNull(controller, "Controller should not be null");
        Assert.notNull(methodName, "Method name should not be null");
        Assert.notNull(rel, "Relation should not be null");

        // Prepare method parameters
        Class<?>[] parameterTypes = null;
        List<Object> parameterValues = null;
        if (methodParams != null) {
            parameterTypes = new Class<?>[methodParams.length];
            parameterValues = new ArrayList<>();
            for (int i = 0; i < methodParams.length; i++) {
                parameterTypes[i] = methodParams[i].getParameterType();
                parameterValues.add(methodParams[i].getValue());
            }
        }

        try {
            Method method = getMethod(controller, methodName, parameterTypes);
            C proxyControllerInstance = ControllerLinkBuilder.methodOn(controller);
            Object invoke;
            if (parameterValues != null) {
                invoke = method.invoke(proxyControllerInstance, parameterValues.toArray());
            } else {
                invoke = method.invoke(proxyControllerInstance);
            }
            return ControllerLinkBuilder.linkTo(invoke).withRel(rel);
        } catch (MethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // Do not insert link
            LOGGER.trace("HATEOAS link skipped silently due to introspection error or access denied", e);
        }
        return null;
    }

    /**
     * Retrieve a method from a class
     * @param pController class
     * @param pMethodName method name
     * @param pParameterTypes parameter types
     * @return associated {@link Method}
     * @throws MethodException if method cannot be retrieved
     */
    private Method getMethod(final Class<?> pController, final String pMethodName, final Class<?>... pParameterTypes)
            throws MethodException {
        try {
            final Method method = pController.getMethod(pMethodName, pParameterTypes);
            checkAuthorization(method);
            return method;
        } catch (final NoSuchMethodException e) {
            final String message = MessageFormat
                    .format("No such method {0} in controller {1}.", pMethodName, pController.getCanonicalName());
            LOGGER.error(message, e);
            throw new MethodException(message);
        } catch (final SecurityException e) {
            final String message = MessageFormat
                    .format("Security exception accessing method {0} in controller {1}.", pMethodName,
                            pController.getCanonicalName());
            LOGGER.error(message, e);
            throw new MethodException(message);
        }
    }

    /**
     * Check if method is accessible regarding security authorities
     * @param pMethod method to check
     * @throws MethodException if method not authorized
     */
    private void checkAuthorization(final Method pMethod) throws MethodException {
        final JWTAuthentication auth = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();

        try {
            accessDecisionManager.decide(auth, new SimpleMethodInvocation(null, pMethod), null);
        } catch (AccessDeniedException | InsufficientAuthenticationException e) {
            final String message = MessageFormat.format("Unauthorized method {0}", pMethod.getName());
            LOGGER.debug(message);
            LOGGER.trace(message, e);
            throw new MethodException(message);
        }
    }
}
