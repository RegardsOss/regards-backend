/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/**
 *
 * Post process plugin instances to inject annotated parameters.
 *
 * @author cmertz
 */
public abstract class AbstractPluginParametersUtil {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPluginParametersUtil.class);

    /**
     * PrimitiveObject for the plugin parameters
     * 
     * @author cmertz
     *
     */
    private enum PrimitiveObject {
        STRING(String.class), BYTE(Byte.class), SHORT(Short.class), INT(Integer.class), LONG(Long.class), FLOAT(
                Float.class), DOUBLE(Double.class), BOOLEAN(Boolean.class);

        /**
         * Type_
         */
        private final Class<?> type;

        /**
         * Constructor
         * 
         * @param pType
         *            primitive type
         *
         * @since 1.0-SNAPSHOT
         */
        private PrimitiveObject(Class<?> pType) {
            this.type = pType;
        }

        /**
         * Get method.
         *
         * @return the type
         * @since 1.0-SNAPSHOT
         */
        public Class<?> getType() {
            return type;
        }
    }

    /**
     *
     * Retrieve plugin parameters by reflection on class fields
     *
     * @param pPluginClass
     *            the target class
     * @return list of parameters or null
     */
    public static List<String> getParameters(Class<?> pPluginClass) {
        List<String> parameters = null;
        for (final Field field : pPluginClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                boolean isSupportedType = false;

                /*
                 * Is the field has a primitive type
                 */
                isSupportedType = isAPrimitiveType(field);

                if (!isSupportedType) {
                    /*
                     * Is the field has an interface plugin type
                     */
                    isSupportedType = isAnInterface(field);
                }

                if (isSupportedType) {
                    if (parameters == null) {
                        parameters = new ArrayList<String>();
                    }
                    // Get annotation and add parameter
                    final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);
                    parameters.add(pluginParameter.name());
                }

                /*
                 * The type of the field is unknown
                 */
                if (!isSupportedType) {
                    if (LOGGER.isWarnEnabled()) {
                        final StringBuffer str = new StringBuffer();
                        str.append(String.format("Annotation \"%s\" not applicable for field type \"%s\"",
                                                 PluginParameter.class.getName(), field.getType()));
                        str.append(". System will ignore it.");
                        LOGGER.warn(str.toString());
                    }
                }
            }
        }
        return parameters;
    }

    /**
     * Search a field in the {@link PrimitiveObject}
     * 
     * @param pField
     *            a field
     * @return true is the type of the field is a {@link PrimitiveObject}
     */
    private static boolean isAPrimitiveType(Field pField) {
        boolean isSupportedType = Arrays.asList(PrimitiveObject.values()).stream()
                .filter(s -> pField.getType().isAssignableFrom(s.getType())) != null;
        // // Register supported parameters
//        boolean isSupportedType = false;
//        for (final PrimitiveObject typeWrapper : PrimitiveObject.values()) {
//            if (pField.getType().isAssignableFrom(typeWrapper.getType())) {
//                isSupportedType = true;
//                break;
//            }
//        }

        return isSupportedType;
    }

    /**
     * Search a field like a {@link PluginInterface}
     * 
     * @param pField
     *            a field
     * @return true is the type of the field is a {@link PrimitiveObject}
     */
    private static boolean isAnInterface(Field pField) {
        boolean isSupportedType = false;
        LOGGER.info("name :" + pField.getType().getName());
        LOGGER.info("type :" + pField.getType());

        final List<String> pluginInterfaces = AbstractPluginInterfaceUtils
                .getInterfaces("fr.cnes.regards.plugins.utils");

        if (pluginInterfaces != null && pluginInterfaces.size() > 0) {
            isSupportedType = pluginInterfaces.stream().filter(s -> s.equalsIgnoreCase(pField.getType().getName()))
                    .count() > 0;
        }

        return isSupportedType;
    }

    /**
     *
     * Use configured values to set field values.
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pPluginConfiguration
     *            the plugin configuration
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcess(T pPluginInstance, PluginConfiguration pPluginConfiguration)
            throws PluginUtilsException, NoSuchElementException {
        // Look for annotated fields
        for (final Field field : pPluginInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);
                // Get configurated value
                final String paramVal = pPluginConfiguration.getParameterValue(pluginParameter.name());
                // Inject value
                AbstractReflectionUtils.makeAccessible(field);

                // TODO CMZ gérer les 2 cas PrimitiveObject et interface comme ci-dessus pour les tests
                for (final PrimitiveObject typeWrapper : PrimitiveObject.values()) {
                    if (field.getType().isAssignableFrom(typeWrapper.getType())) {
                        try {
                            final Object effectiveVal;
                            if (typeWrapper.getType().equals(String.class)) {
                                effectiveVal = paramVal;
                            } else {
                                final Method method = typeWrapper.getType().getDeclaredMethod("valueOf", String.class);
                                effectiveVal = method.invoke(null, paramVal);
                            }
                            field.set(pPluginInstance, effectiveVal);
                            break;
                        } catch (final IllegalAccessException | NoSuchMethodException | SecurityException
                                | IllegalArgumentException | InvocationTargetException e) {
                            LOGGER.error(String
                                    .format("Exception while processing param \"%s\" in plugin class \"%s\" with value \"%s\".",
                                            pluginParameter.name(), pPluginInstance.getClass(), paramVal),
                                         e);
                            // Propagate exception
                            throw new PluginUtilsException(e);
                        }
                    }
                }

            }
        }
    }
}
