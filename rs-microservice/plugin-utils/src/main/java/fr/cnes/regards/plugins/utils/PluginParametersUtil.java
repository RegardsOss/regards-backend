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
     * @param pReturnPlugin
     *            the plugin instance
     * @param pPluginConfiguration
     *            the plugin configuration
     * @param pPluginParameters
     *            an optional set of {@link fr.cnes.regards.modules.plugins.domain.PluginParameter}
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcess(T pReturnPlugin, PluginConfiguration pPluginConfiguration,
            fr.cnes.regards.modules.plugins.domain.PluginParameter... pPluginParameters) throws PluginUtilsException {
        LOGGER.debug("Starting postProcess :" + pReturnPlugin.getClass().getSimpleName());

        // Look for annotated fields
        for (final Field field : pReturnPlugin.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter pluginParameterAnnotation = field.getAnnotation(PluginParameter.class);
                processPluginParameter(pReturnPlugin, pPluginConfiguration, field, pluginParameterAnnotation,
                                       pPluginParameters);
            }
        }

        LOGGER.debug("Ending postProcess :" + pReturnPlugin.getClass().getSimpleName());
    }

    /**
     * Use configured values to set field value for a {@link PluginParameter}
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pPluginConfiguration
     *            the plugin configuration to used
     * @param pField
     *            the parameter
     * @param pPluginParameterAnnotation
     *            the plugin parameter
     * @param pPluginParameters
     *            an optional set of {@link fr.cnes.regards.modules.plugins.domain.PluginParameter}
     * @throws PluginUtilsException
     *             if any error occurs
     */
    private static <T> void processPluginParameter(T pPluginInstance, PluginConfiguration pPluginConfiguration,
            Field pField, PluginParameter pPluginParameterAnnotation,
            fr.cnes.regards.modules.plugins.domain.PluginParameter... pPluginParameters) throws PluginUtilsException {

        // Inject value
        AbstractReflectionUtils.makeAccessible(pField);

        // Try to get a primitve type for the current parameter
        final Optional<PrimitiveObject> typeWrapper = isAPrimitiveType(pField);

        if (typeWrapper.isPresent()) {
            // The parameter is a primitive type
            LOGGER.debug("primitive parameter:" + pField.getName() + " -> " + pField.getType());

            postProcessPrimitiveType(pPluginInstance, pPluginConfiguration, pField, typeWrapper,
                                     pPluginParameterAnnotation, pPluginParameters);
        } else {
            if (isAnInterface(pField)) {
                // The wrapper is an interface plugin type
                LOGGER.debug("interface parameter:" + pField.getName() + " -> " + pField.getType());

                postProcessInterface(pPluginInstance, pPluginConfiguration, pField, pPluginParameterAnnotation);
            } else {
                throw new PluginUtilsException("Type parameter unknown.");
            }
        }

    }

    /**
     * Use configured values to set field values for a parameter of type {@link PrimitiveObject}
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pPluginConfiguration
     *            the plugin configuration to used
     * @param pField
     *            the parameter
     * @param pTypeWrapper
     *            the type wrapper of the parameter
     * @param pPluginParameterAnnotation
     *            the plugin parameter
     * @param pPluginParameters
     *            an optional set of {@link fr.cnes.regards.modules.plugins.domain.PluginParameter}
     * 
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcessPrimitiveType(T pPluginInstance, PluginConfiguration pPluginConfiguration,
            Field pField, Optional<PrimitiveObject> pTypeWrapper, PluginParameter pPluginParameterAnnotation,
            fr.cnes.regards.modules.plugins.domain.PluginParameter... pPluginParameters) throws PluginUtilsException {

        LOGGER.debug("Starting postProcessPrimitiveType :" + pPluginParameterAnnotation.name());

        // Get setup value
        String paramValue = pPluginConfiguration.getParameterValue(pPluginParameterAnnotation.name());

        /*
         * Test if a specific value is given for this annotation parameter
         */
        final Optional<fr.cnes.regards.modules.plugins.domain.PluginParameter> aDynamicPluginParameter = Arrays
                .asList(pPluginParameters).stream().filter(s -> s.getName().equals(pPluginParameterAnnotation.name()))
                .findFirst();
        if (aDynamicPluginParameter.isPresent()) {
            /*
             * Test if this parameter is set as dynamic in the plugin configuration
             * 
             */
            final Optional<fr.cnes.regards.modules.plugins.domain.PluginParameter> configuratedPluginParameter = pPluginConfiguration
                    .getParameters().stream()
                    .filter(s -> s.getName().equals(aDynamicPluginParameter.get().getName()) && s.getIsDynamic())
                    .findFirst();
            if (configuratedPluginParameter.isPresent()) {
                if (configuratedPluginParameter.get().getDynamicsValues() == null
                        || (configuratedPluginParameter.get().getDynamicsValues() != null
                                && !configuratedPluginParameter.get().getDynamicsValues().isEmpty()
                                && Arrays.asList(configuratedPluginParameter.get().getDynamicsValues()).stream()
                                        .filter(s -> s.equals(aDynamicPluginParameter.get().getValue())).findAny()
                                        .isPresent())
                        || (configuratedPluginParameter.get().getDynamicsValues().isEmpty())) {
                    paramValue = aDynamicPluginParameter.get().getValue();
                }
            }
        }

        LOGGER.debug("parameter value : " + paramValue);

        try {
            final Object effectiveVal;
            if (pTypeWrapper.get().getType().equals(String.class)) {
                effectiveVal = paramValue;
            } else {
                final Method method = pTypeWrapper.get().getType().getDeclaredMethod("valueOf", String.class);
                effectiveVal = method.invoke(null, paramValue);
            }
            pField.set(pPluginInstance, effectiveVal);
        } catch (final IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.error(String.format(
                                       "Exception while processing param \"%s\" in plugin class \"%s\" with value \"%s\".",
                                       pPluginParameterAnnotation.name(), pPluginInstance.getClass(), paramValue),
                         e);
            // Propagate exception
            throw new PluginUtilsException(e);
        }
        LOGGER.debug("Ending   postProcessPrimitiveType :" + pPluginParameterAnnotation.name());
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PluginParameter}
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pPluginConfiguration
     *            the plugin configuration to used
     * @param pField
     *            the parameter
     * @param pPluginParameterAnnotation
     *            the plugin parameter
     * 
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcessInterface(T pPluginInstance, PluginConfiguration pPluginConfiguration,
            Field pField, PluginParameter pPluginParameterAnnotation) throws PluginUtilsException {

        LOGGER.debug("Starting postProcessInterface :" + pPluginParameterAnnotation.name());

        // Get setup value
        final PluginConfiguration paramValue = pPluginConfiguration
                .getParameterConfiguration(pPluginParameterAnnotation.name());

        LOGGER.debug("parameter value : " + paramValue);

        try {
            final Object effectiveVal = AbstractPluginUtils.getPlugin(paramValue, paramValue.getPluginMetaData());
            pField.set(pPluginInstance, effectiveVal);
        } catch (PluginUtilsException | IllegalArgumentException | IllegalAccessException e) {
            LOGGER.error(String.format(
                                       "Exception while processing param \"%s\" in plugin class \"%s\" with value \"%s\".",
                                       pPluginParameterAnnotation.name(), pPluginInstance.getClass(), paramValue),
                         e);
            // Propagate exception
            throw new PluginUtilsException(e);
        }

        LOGGER.debug("Ending   postProcessInterface :" + pPluginParameterAnnotation.name());
    }
}
