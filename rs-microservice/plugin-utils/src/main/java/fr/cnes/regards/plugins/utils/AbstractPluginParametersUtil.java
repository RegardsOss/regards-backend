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
import java.util.Optional;

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

                if (isAPrimitiveType(field).isPresent() || isAnInterface(field)) {
                    if (parameters == null) {
                        parameters = new ArrayList<String>();
                    }
                    // Get annotation and add parameter
                    final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);
                    parameters.add(pluginParameter.name());
                } else {
                    /*
                     * The type of the field is unknown
                     */
                    if (LOGGER.isWarnEnabled()) {
                        final StringBuilder str = new StringBuilder();
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
     * @return an {@link Optional} {@link PrimitiveObject}
     */
    private static Optional<PrimitiveObject> isAPrimitiveType(Field pField) {
        return Arrays.asList(PrimitiveObject.values()).stream()
                .filter(s -> pField.getType().isAssignableFrom(s.getType())).findFirst();
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
        final List<String> pluginInterfaces = AbstractPluginInterfaceUtils
                .getInterfaces("fr.cnes.regards.plugins.utils");

        if (pluginInterfaces != null && !pluginInterfaces.isEmpty()) {
            isSupportedType = pluginInterfaces.stream().filter(s -> s.equalsIgnoreCase(pField.getType().getName()))
                    .count() > 0;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("parameter interface :" + pField.toGenericString() + " -> " + isSupportedType);
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
            throws PluginUtilsException {
        LOGGER.debug("Starting postProcess :" + pPluginInstance.getClass().getSimpleName());
        // Look for annotated fields
        for (final Field field : pPluginInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);

                // Inject value
                AbstractReflectionUtils.makeAccessible(field);

                // Try to get a primitve type for the current parameter
                final Optional<PrimitiveObject> typeWrapper = isAPrimitiveType(field);

                if (typeWrapper.isPresent()) {
                    // The parameter is a primtive type
                    LOGGER.debug("primitive parameter:" + field.getName() + " -> " + field.getType());

                    // Get configurated value
                    final String paramVal = pPluginConfiguration.getParameterValue(pluginParameter.name());

                    postProcessPrimitiveType(pPluginInstance, field, typeWrapper, pluginParameter.name(), paramVal);
                } else {
                    if (isAnInterface(field)) {
                        // The wrapper is an interface plugin type
                        LOGGER.debug("interface parameter:" + field.getName() + " -> " + field.getType());

                        // Get configurated value
                        final PluginConfiguration paramVal = pPluginConfiguration
                                .getParameterConfiguration(pluginParameter.name());

                        postProcessInterface(pPluginInstance, field, pluginParameter.name(), paramVal);
                    } else {
                        throw new PluginUtilsException("Type parameter unknown.");
                    }
                }
            }
        }
        LOGGER.debug("Ending postProcess :" + pPluginInstance.getClass().getSimpleName());
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PrimitiveObject}
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pField
     *            the parameter
     * @param pTypeWrapper
     *            the type wrapper of the parameter
     * @param pParameterName
     *            the parameter name
     * @param pParameterValue
     *            the parameter value
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcessPrimitiveType(T pPluginInstance, Field pField,
            Optional<PrimitiveObject> pTypeWrapper, String pParameterName, String pParameterValue)
            throws PluginUtilsException {
        LOGGER.debug("Starting postProcessPrimitiveType :" + pParameterName + " - param value : " + pParameterValue);
        try {
            final Object effectiveVal;
            if (pTypeWrapper.get().getType().equals(String.class)) {
                effectiveVal = pParameterValue;
            } else {
                final Method method = pTypeWrapper.get().getType().getDeclaredMethod("valueOf", String.class);
                effectiveVal = method.invoke(null, pParameterValue);
            }
            pField.set(pPluginInstance, effectiveVal);
        } catch (final IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.error(String.format(
                                       "Exception while processing param \"%s\" in plugin class \"%s\" with value \"%s\".",
                                       pParameterName, pPluginInstance.getClass(), pParameterValue),
                         e);
            // Propagate exception
            throw new PluginUtilsException(e);
        }
        LOGGER.debug("Ending   postProcessPrimitiveType :" + pParameterName);
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PluginParameter}
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pField
     *            the parameter
     * @param pParameterName
     *            the parameter name
     * @param pParameterValue
     *            the parameter value
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcessInterface(T pPluginInstance, Field pField, String pParameterName,
            PluginConfiguration pParameterValue) throws PluginUtilsException {
        LOGGER.debug("Starting postProcessInterface :" + pParameterName + " - param value : "
                + pParameterValue.toString());

        try {
            final Object effectiveVal = AbstractPluginUtils.getPlugin(pParameterValue,
                                                                      pParameterValue.getPluginMetaData());
            pField.set(pPluginInstance, effectiveVal);
        } catch (PluginUtilsException | IllegalArgumentException | IllegalAccessException e) {
            LOGGER.error(String.format(
                                       "Exception while processing param \"%s\" in plugin class \"%s\" with value \"%s\".",
                                       pParameterName, pPluginInstance.getClass(), pParameterValue),
                         e);
            // Propagate exception
            throw new PluginUtilsException(e);
        }

        LOGGER.debug("Ending   postProcessInterface :" + pParameterName);
    }
}
