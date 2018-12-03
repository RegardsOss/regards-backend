package fr.cnes.regards.framework.test.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import fr.cnes.regards.framework.utils.RsRuntimeException;

public final class Beans {

    private static final Logger LOGGER = LoggerFactory.getLogger(Beans.class);

    private Beans() {
    }

    /**
     * Compare two objects first with areEqual then by their properties readers, recursively follow inner objects using
     * same comparison mechanism.
     * <b>Beware of collections or arrays, in this case, native areEqual is used.</b>
     * @param pO1 first object to compare
     * @param pO2 second object to compare
     * @return true when objects equal, false otherwise
     */
    @SuppressWarnings("rawtypes")
    public static boolean areEqual(final Object pO1, final Object pO2, String... gettersToForget) {
        if ((pO1 == null) && (pO2 == null)) {
            return true;
        } else if ((pO1 == null) || (pO2 == null)) {
            LOGGER.warn("Objects differ : one is null, not the other");
            return false;
        }

        if (pO1.getClass().isArray() && (pO2.getClass().isArray())) {
            return Arrays.equals((Object[]) pO1, (Object[]) pO2);
        }
        if (!Objects.equal(pO1, pO2)) {
            LOGGER.warn("Objects differ : {} vs {}", pO1, pO2);
            return false;
        }

        if (pO1.getClass() != pO2.getClass()) {
            throw new IllegalArgumentException("Both objects must be of same class");
        }
        // Particular case of "base" types
        if ((pO1 instanceof Number) || (pO1 instanceof String) || (pO1 instanceof Character)) {
            return pO1.equals(pO2);
        }
        // Find all properties
        try {
            BeanInfo info1 = Introspector.getBeanInfo(pO1.getClass(), Object.class);
            if ((info1.getMethodDescriptors() == null) || (info1.getMethodDescriptors().length == 0)) {
                return true;
            }
            // For all externally accessible methods
            for (MethodDescriptor methodDesc : info1.getMethodDescriptors()) {
                Method method = methodDesc.getMethod();
                // if it is a read property method (starting by get or is and without any parameter)
                if ((method.getParameterCount() == 0) && (method.getName().startsWith("get") || method.getName()
                        .startsWith("is"))) {
                    if (contains(gettersToForget, method.getName())) {
                        continue;
                    }
                    Object v1 = method.invoke(pO1, new Object[0]);
                    Object v2 = method.invoke(pO2, new Object[0]);
                    if (v1 != null) {
                        if (v1.getClass().getName().startsWith("fr.cnes")) {
                            if (!Beans.areEqual(v1, v2, gettersToForget)) {
                                LOGGER.warn("Objects differ : {}.{} : {} vs {}.{} : {}", pO1, method.getName(), v1, pO2,
                                            method.getName(), v2);
                                return false;
                            }
                        } else if (v1 instanceof Collection) { // For Hb9n PersistentBag type which seems to not be compatible with collections
                            if (!Beans.areEqual(((Collection) v1).toArray(), ((Collection) v2).toArray(),
                                                gettersToForget)) {
                                LOGGER.warn("Object collections differ : {} vs {}",
                                            Arrays.toString(((Collection) v1).toArray()), ((Collection) v2).toArray());
                                return false;
                            }
                        } else {
                            if (!v1.equals(v2)) {
                                LOGGER.warn("Objects differ : {}.{} : {} vs {}.{} : {}", pO1, method.getName(), v1, pO2,
                                            method.getName(), v2);
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new RsRuntimeException(e);
        }
    }

    public static boolean contains(String[] tab, String valueToSearch) {
        for (String v : tab) {
            if (v.equals(valueToSearch)) {
                return true;
            }
        }
        return false;
    }
}
