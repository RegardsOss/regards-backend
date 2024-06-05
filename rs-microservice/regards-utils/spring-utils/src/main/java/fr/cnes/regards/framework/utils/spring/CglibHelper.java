/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.spring;

import org.springframework.aop.TargetSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class to retrieve proxied object created through AOP.<br/>
 * This is necessary to permit spying an autowired (final) bean with Mockito (which doesn't like final objects to be spyed).
 *
 * @author Olivier Rousselot
 */
public final class CglibHelper {

    /**
     * Retrieve cglib proxied object
     *
     * @param proxied cglib proxy object
     * @param <T>     proxied class
     * @return proxied object
     */
    public static <T> T getTargetObject(T proxied) {
        String name = proxied.getClass().getName();
        if (name.toLowerCase().contains("cglib")) {
            try {
                return (T) findSpringTargetSource(proxied).getTarget();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return proxied;
    }

    /**
     * Call method "getTargetSource()" on cglib proxy object
     */
    private static TargetSource findSpringTargetSource(Object proxied)
        throws InvocationTargetException, IllegalAccessException {
        Method[] methods = proxied.getClass().getDeclaredMethods();
        Method targetSourceMethod = findTargetSourceMethod(methods, proxied);
        targetSourceMethod.setAccessible(true);
        return (TargetSource) targetSourceMethod.invoke(proxied);
    }

    /**
     * Find method "getTargetSource()" on cglib proxy class
     */
    private static Method findTargetSourceMethod(Method[] methods, Object proxied) {
        for (Method method : methods) {
            if (method.getName().endsWith("getTargetSource")) {
                return method;
            }
        }
        throw new IllegalStateException("Could not find target source method on proxied object ["
                                        + proxied.getClass()
                                        + "]");
    }
}
