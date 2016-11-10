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
 * @author Christophe Mertz
 */
public final class PluginParameterUtils {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterUtils.class);

    /**
     * Default constructor
     */
    private PluginParameterUtils() {

    }

    /**
     * PrimitiveObject for the plugin parameters
     * 
     * @author Christophe Mertz
     *
     */
    public enum PrimitiveObject {
        /**
         * A primitive of {@link String}
         */
        STRING(String.class),

        /**
         * A primitive of {@link Byte}
         */
        BYTE(Byte.class),

        /**
         * A primitive of {@link Short}
         */
        SHORT(Short.class),

        /**
         * A primitive of {@link Integer}
         */
        INT(Integer.class),

        /**
         * A primitive of {@link Long}
         */
        LONG(Long.class),

        /**
         * A primitive of {@link Float}
         */
        FLOAT(Float.class),

        /**
         * A primitive of {@link Double}
         */
        DOUBLE(Double.class),

        /**
         * A primitive of {@link Boolean}
         */
        BOOLEAN(Boolean.class);

        /**
         * Type
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
        private PrimitiveObject(final Class<?> pType) {
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
    public static List<String> getParameters(final Class<?> pPluginClass) {
        List<String> parameters = null;

        for (final Field field : pPluginClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {

                if (isAPrimitiveType(field).isPresent() || isAnInterface(field)) {
                    if (parameters == null) {
                        parameters = new ArrayList<>();
                    }
                    // Get annotation and add parameter
                    final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);
                    parameters.add(pluginParameter.name());
                } else {
                    /*
                     * The type of the field is unknown
                     */
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format(
                                                  "Annotation <%s> not applicable for field type <%s>. System will ignore it.",
                                                  PluginParameter.class.getName(), field.getType()));
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
    private static Optional<PrimitiveObject> isAPrimitiveType(final Field pField) {
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
    private static boolean isAnInterface(final Field pField) {
        boolean isSupportedType = false;
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces("fr.cnes.regards.plugins.utils");

        if (pluginInterfaces != null && !pluginInterfaces.isEmpty()) {
            isSupportedType = pluginInterfaces.stream().filter(s -> s.equalsIgnoreCase(pField.getType().getName()))
                    .count() > 0;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("interface parameter : %s --> %b", pField.toGenericString(), isSupportedType));
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
     * @param pPlgConf
     *            the plugin configuration
     * @param pPlgParam
     *            an optional set of {@link fr.cnes.regards.modules.plugins.domain.PluginParameter}
     * @throws PluginUtilsException
     *             if any error occurs
     */
    public static <T> void postProcess(final T pReturnPlugin, final PluginConfiguration pPlgConf,
            final fr.cnes.regards.modules.plugins.domain.PluginParameter... pPlgParam) throws PluginUtilsException {
        LOGGER.debug("Starting postProcess :" + pReturnPlugin.getClass().getSimpleName());

        // Test if the plugin configuration is active
        if (!pPlgConf.isActive()) {
            throw new PluginUtilsException(String.format("The plugin configuration <%s-%s> is not active.",
                                                         pPlgConf.getId(), pPlgConf.getLabel()));
        }

        // Look for annotated fields
        for (final Field field : pReturnPlugin.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter plgParamAnnotation = field.getAnnotation(PluginParameter.class);
                processPluginParameter(pReturnPlugin, pPlgConf, field, plgParamAnnotation, pPlgParam);
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
     * @param pPlgConf
     *            the plugin configuration to used
     * @param pField
     *            the parameter
     * @param pPlgParamAnnotation
     *            the plugin parameter
     * @param pPlgParam
     *            an optional set of {@link fr.cnes.regards.modules.plugins.domain.PluginParameter}
     * @throws PluginUtilsException
     *             if any error occurs
     */
    private static <T> void processPluginParameter(final T pPluginInstance, final PluginConfiguration pPlgConf,
            final Field pField, final PluginParameter pPlgParamAnnotation,
            fr.cnes.regards.modules.plugins.domain.PluginParameter... pPlgParam) throws PluginUtilsException {

        // Inject value
        ReflectionUtils.makeAccessible(pField);

        // Try to get a primitve type for the current parameter
        final Optional<PrimitiveObject> typeWrapper = isAPrimitiveType(pField);

        if (typeWrapper.isPresent()) {
            // The parameter is a primitive type
            LOGGER.debug(String.format("primitive parameter : %s --> %s", pField.getName(), pField.getType()));
            postProcessPrimitiveType(pPluginInstance, pPlgConf, pField, typeWrapper, pPlgParamAnnotation, pPlgParam);
        } else {
            if (isAnInterface(pField)) {
                // The wrapper is an interface plugin type
                LOGGER.debug(String.format("interface parameter : %s --> %s", pField.getName(), pField.getType()));
                postProcessInterface(pPluginInstance, pPlgConf, pField, pPlgParamAnnotation);
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
     * @param pPlgConf
     *            the plugin configuration to used
     * @param pField
     *            the parameter
     * @param pTypeWrapper
     *            the type wrapper of the parameter
     * @param pPlgParamAnnotation
     *            the plugin parameter
     * @param pPlgParameters
     *            an optional set of {@link fr.cnes.regards.modules.plugins.domain.PluginParameter}
     * 
     * @throws PluginUtilsException
     *             if any error occurs
     */
    private static <T> void postProcessPrimitiveType(T pPluginInstance, PluginConfiguration pPlgConf, Field pField,
            final Optional<PrimitiveObject> pTypeWrapper, PluginParameter pPlgParamAnnotation,
            fr.cnes.regards.modules.plugins.domain.PluginParameter... pPlgParameters) throws PluginUtilsException {

        LOGGER.debug("Starting postProcessPrimitiveType :" + pPlgParamAnnotation.name());

        // Get setup value
        String paramValue = pPlgConf.getParameterValue(pPlgParamAnnotation.name());

        /*
         * Test if a specific value is given for this annotation parameter
         */
        final Optional<fr.cnes.regards.modules.plugins.domain.PluginParameter> aDynamicPlgParam = Arrays
                .asList(pPlgParameters).stream().filter(s -> s.getName().equals(pPlgParamAnnotation.name()))
                .findFirst();
        if (aDynamicPlgParam.isPresent()) {
            /*
             * Test if this parameter is set as dynamic in the plugin configuration
             */
            final Optional<fr.cnes.regards.modules.plugins.domain.PluginParameter> cfd = pPlgConf.getParameters()
                    .stream().filter(s -> s.getName().equals(aDynamicPlgParam.get().getName()) && s.isDynamic())
                    .findFirst();
            if (cfd.isPresent()) {
                paramValue = postProcessDynamicValues(paramValue, cfd, aDynamicPlgParam);
            }
        }

        LOGGER.debug(String.format("primitive parameter value : %s", paramValue));

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
            // Propagate exception
            throw new PluginUtilsException(
                    String.format("Exception while processing param <%s> in plugin class <%s> with value <%s>.",
                                  pPlgParamAnnotation.name(), pPluginInstance.getClass(), paramValue),
                    e);
        }
        LOGGER.debug("Ending postProcessPrimitiveType :" + pPlgParamAnnotation.name());
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PluginParameter}
     * 
     * @param <T>
     *            a plugin type
     * @param pPluginInstance
     *            the plugin instance
     * @param pPlgConf
     *            the plugin configuration to used
     * @param pField
     *            the parameter
     * @param pPlgParamAnnotation
     *            the plugin parameter
     * 
     * @throws PluginUtilsException
     *             if any error occurs
     */
    private static <T> void postProcessInterface(T pPluginInstance, PluginConfiguration pPlgConf, Field pField,
            PluginParameter pPlgParamAnnotation) throws PluginUtilsException {

        LOGGER.debug("Starting postProcessInterface :" + pPlgParamAnnotation.name());

        // Get setup value
        final PluginConfiguration paramValue = pPlgConf.getParameterConfiguration(pPlgParamAnnotation.name());

        LOGGER.debug(String.format("interface parameter value : %s", paramValue));

        try {
            final Object effectiveVal = PluginUtils.getPlugin(paramValue, paramValue.getPluginClassName());
            pField.set(pPluginInstance, effectiveVal);
        } catch (PluginUtilsException | IllegalArgumentException | IllegalAccessException e) {
            // Propagate exception
            throw new PluginUtilsException(
                    String.format("Exception while processing param <%s> in plugin class <%s> with value <%s>.",
                                  pPlgParamAnnotation.name(), pPluginInstance.getClass(), paramValue),
                    e);
        }

        LOGGER.debug("Ending postProcessInterface :" + pPlgParamAnnotation.name());
    }

    /**
     * Apply a dynamic parameter value to a parameter plugin
     * 
     * @param pParamValue
     *            the current parameter value
     * @param pConfiguredPlgParam
     *            the plugin parameter configured
     * @param pDynamicPlgParam
     *            the dynamic parameter value
     * @return the new parameter value
     * @throws PluginUtilsException
     *             if any error occurs
     */
    private static String postProcessDynamicValues(final String pParamValue,
            final Optional<fr.cnes.regards.modules.plugins.domain.PluginParameter> pConfiguredPlgParam,
            final Optional<fr.cnes.regards.modules.plugins.domain.PluginParameter> pDynamicPlgParam)
            throws PluginUtilsException {
        final String paramValue;

        LOGGER.debug(String.format("Starting postProcessDynamicValues : %s - init value= <%s>",
                                   pDynamicPlgParam.get().getName(), pParamValue));

        if ((pConfiguredPlgParam.get().getDynamicsValues() != null)
                && (!pConfiguredPlgParam.get().getDynamicsValues().isEmpty()) && (!pConfiguredPlgParam.get()
                        .getDynamicsValuesAsString().contains(pDynamicPlgParam.get().getValue()))) {
            // The dynamic parameter value is not a possible value
            throw new PluginUtilsException(
                    String.format("The dynamic value <%s> is not an authorized value for the parameter %s.",
                                  pDynamicPlgParam.get().getValue(), pDynamicPlgParam.get().getName()));
        }

        paramValue = pDynamicPlgParam.get().getValue();

        LOGGER.debug(String.format("Starting postProcessDynamicValues : %s - new value= <%s>",
                                   pDynamicPlgParam.get().getName(), pParamValue));

        return paramValue;
    }
}
