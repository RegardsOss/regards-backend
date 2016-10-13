/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * Utility class used in reflections
 *
 * @author cmertz
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
    public static void makeAccessible(Field pField) {
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
    public static void makeAccessible(Method pMethod) {
        if ((!Modifier.isPublic(pMethod.getModifiers())
                || !Modifier.isPublic(pMethod.getDeclaringClass().getModifiers())
                || Modifier.isFinal(pMethod.getModifiers())) && !pMethod.isAccessible()) {
            pMethod.setAccessible(true);
        }
    }

}
