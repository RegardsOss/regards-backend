/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

/**
 *
 * Post process plugin instances to inject annotated parameters.
 *
 * @author msordi
 * @since 1.0-SNAPSHOT
 */
public abstract class PluginParametersUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(PluginParametersUtil.class);

    private enum PrimitiveObject {
        STRING(String.class), BYTE(Byte.class), SHORT(Short.class), INT(Integer.class), LONG(Long.class), FLOAT(
                Float.class), DOUBLE(Double.class), BOOLEAN(Boolean.class);

        private final Class<?> type_;

        /**
         * Constructor
         *
         * @since 1.0-SNAPSHOT
         */
        private PrimitiveObject(Class<?> pType) {
            this.type_ = pType;
        }

        /**
         * Get method.
         *
         * @return the type
         * @since 1.0-SNAPSHOT
         */
        public Class<?> getType() {
            return type_;
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
            if (field.isAnnotationPresent(fr.cnes.regards.modules.plugins.annotations.PluginParameter.class)) {
                boolean isSupportedType = false;

                // Register supported parameters
                for (final PrimitiveObject typeWrapper : PrimitiveObject.values()) {
                    if (field.getType().isAssignableFrom(typeWrapper.getType())) {

                        isSupportedType = true;
                        // Init parameters if necessary
                        if (parameters == null) {
                            parameters = new ArrayList<String>();
                        }
                        // Get annotation and add parameter
                        final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);
                        parameters.add(pluginParameter.name());
                        break;
                    }
                }

                if (!isSupportedType) {
                    LOGGER.warn(String.format(
                                              "Annotation \"%s\" not applicable for field type \"%s\". System will ignore it.",
                                              PluginParameter.class.getName(), field.getType()));
                }
            }
        }
        return parameters;
    }

    /**
     *
     * Use configured values to set field values.
     *
     * @param pPluginInstance
     *            the plugin instance
     * @param pPluginConfiguration
     *            the plugin configuration
     * @throws PluginUtilsException
     *             if any error occurs
     * @since 1.0-SNAPSHOT
     */
    public static <T> void postProcess(T pPluginInstance, PluginConfiguration pPluginConfiguration)
            throws PluginUtilsException {

        // Look for annotated fields
        for (final Field field : pPluginInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);
                // Get configurated value
                final String paramVal = pPluginConfiguration.getParameterValue(pluginParameter.name());
                // Inject value
                ReflectionUtils.makeAccessible(field);
                for (final PrimitiveObject typeWrapper : PrimitiveObject.values()) {
                    if (field.getType().isAssignableFrom(typeWrapper.getType())) {
                        try {
                            Object effectiveVal;
                            if (typeWrapper.getType().equals(String.class)) {
                                effectiveVal = paramVal;
                            }
                            else {
                                final Method method = typeWrapper.getType().getDeclaredMethod("valueOf", String.class);
                                effectiveVal = method.invoke(null, paramVal);
                            }
                            field.set(pPluginInstance, effectiveVal);
                            break;
                        }
                        catch (final Exception e) {
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
