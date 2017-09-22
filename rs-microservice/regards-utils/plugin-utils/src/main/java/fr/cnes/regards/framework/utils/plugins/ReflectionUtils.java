/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.framework.utils.plugins;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * Utility class used in reflections
 *
 * @author Christophe Mertz
 */
public final class ReflectionUtils {

    /**
     * A private constructor
     */
    private ReflectionUtils() {
        super();
    }

    /**
     *
     * Make a field accessible
     *
     * @param pField
     *            the field to check
     */
    public static void makeAccessible(final Field pField) {
        if ((!Modifier.isPublic(pField.getModifiers()) || !Modifier.isPublic(pField.getDeclaringClass().getModifiers())
                || Modifier.isFinal(pField.getModifiers())) && !pField.isAccessible()) {
            pField.setAccessible(true);
        }
    }

    /**
     *
     * Make a method accessible
     *
     * @param pMethod
     *            the field to check
     */
    public static void makeAccessible(final Method pMethod) {
        if ((!Modifier.isPublic(pMethod.getModifiers())
                || !Modifier.isPublic(pMethod.getDeclaringClass().getModifiers())
                || Modifier.isFinal(pMethod.getModifiers())) && !pMethod.isAccessible()) {
            pMethod.setAccessible(true);
        }
    }

}
