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

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType.ParamType;

/**
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
         * @param pType primitive type
         * @since 1.0-SNAPSHOT
         */
        private PrimitiveObject(final Class<?> pType) {
            type = pType;
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
     * Retrieve plugin parameters by reflection on class fields
     *
     * @param pPluginClass the target class
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return list of parameters or null
     */
    public static List<PluginParameterType> getParameters(final Class<?> pPluginClass, final List<String> pPrefixs) {
        List<PluginParameterType> parameters = null;

        for (final Field field : pPluginClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {

                boolean aPrimitiveType = isAPrimitiveType(field).isPresent();

                if (aPrimitiveType || isAnInterface(field, pPrefixs)) {
                    if (parameters == null) {
                        parameters = new ArrayList<>();
                    }
                    parameters.add(buildPluginParameter(field, aPrimitiveType));
                } else {
                    /*
                     * The type of the field is unknown
                     */
                    LOGGER.warn(
                            String.format("Annotation <%s> not applicable for field type <%s>. System will ignore it.",
                                          PluginParameter.class.getName(), field.getType()));
                }
            }
        }
        return parameters;
    }

    /**
     * Build a {@link PluginParameterType}.
     *
     * @param pField The current {@link Field}
     * @param pIsPrimitive True if the type of the field is a Java primitive type False otherwise
     * @return a {@link PluginParameterType}
     */
    private static PluginParameterType buildPluginParameter(Field pField, boolean pIsPrimitive) {
        // Get annotation
        final PluginParameter pluginParameter = pField.getAnnotation(PluginParameter.class);

        PluginParameterType paramType;
        if (pIsPrimitive) {
            paramType = new PluginParameterType(pluginParameter.name(), pField.getType().getName(),
                                                pluginParameter.defaultValue(), pluginParameter.optional(),
                                                ParamType.PRIMITIVE);
        } else {
            paramType = new PluginParameterType(pluginParameter.name(), pField.getType().getName(), ParamType.PLUGIN);
        }

        return paramType;
    }

    /**
     * Search a field in the {@link PrimitiveObject}
     *
     * @param pField a field
     * @return an {@link Optional} {@link PrimitiveObject}
     */
    private static Optional<PrimitiveObject> isAPrimitiveType(final Field pField) {
        return Arrays.asList(PrimitiveObject.values()).stream()
                .filter(s -> pField.getType().isAssignableFrom(s.getType())).findFirst();
    }

    /**
     * Search a field like a {@link PluginInterface}
     *
     * @param pField a field
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return true is the type of the field is a {@link Plugin}
     */
    private static boolean isAnInterface(final Field pField, final List<String> pPrefixs) {
        boolean isInterface = false;
        final ListIterator<String> listIter = pPrefixs.listIterator();

        while (listIter.hasNext() && !isInterface) {
            final String s = listIter.next();
            isInterface = isAnInterface(pField, s);
        }

        return isInterface;
    }

    /**
     * Search a field like a {@link PluginInterface}
     *
     * @param pField a field
     * @param pPrefix a package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return true is the type of the field is a {@link PrimitiveObject}
     */
    private static boolean isAnInterface(final Field pField, final String pPrefix) {
        boolean isSupportedType = false;
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(pPrefix);

        if ((pluginInterfaces != null) && !pluginInterfaces.isEmpty()) {
            isSupportedType =
                    pluginInterfaces.stream().filter(s -> s.equalsIgnoreCase(pField.getType().getName())).count() > 0;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("interface parameter : %s --> %b", pField.toGenericString(), isSupportedType));
        }

        return isSupportedType;
    }

    /**
     * Use configured values to set field values.
     *
     * @param <T> a plugin type
     * @param pReturnPlugin the plugin instance
     * @param pPlgConf the plugin configuration
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param instantiatedPluginMap
     * @param pPlgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter} @ if any error occurs
     */
    public static <T> void postProcess(final T pReturnPlugin, final PluginConfiguration pPlgConf,
            final List<String> pPrefixs, Map<Long, Object> instantiatedPluginMap,
            final fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... pPlgParameters) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting postProcess :" + pReturnPlugin.getClass().getSimpleName());
        }

        // Test if the plugin configuration is active
        if (!pPlgConf.isActive()) {
            throw new PluginUtilsRuntimeException(
                    String.format("The plugin configuration <%s-%s> is not active.", pPlgConf.getId(),
                                  pPlgConf.getLabel()));
        }

        // Look for annotated fields
        for (final Field field : pReturnPlugin.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter plgParamAnnotation = field.getAnnotation(PluginParameter.class);
                processPluginParameter(pReturnPlugin, pPlgConf, field, plgParamAnnotation, pPrefixs,
                                       instantiatedPluginMap, pPlgParameters);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ending postProcess :" + pReturnPlugin.getClass().getSimpleName());
        }
    }

    /**
     * Use configured values to set field value for a {@link PluginParameter}
     *
     * @param <T> a plugin type
     * @param pPluginInstance the plugin instance
     * @param pPlgConf the plugin configuration to used
     * @param pField the parameter
     * @param pPlgParamAnnotation the plugin parameter
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param instantiatedPluginMap already instantiated plugins
     * @param pPlgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter} @ if any error occurs
     */
    private static <T> void processPluginParameter(T pPluginInstance, PluginConfiguration pPlgConf,
            Field pField, PluginParameter pPlgParamAnnotation, List<String> pPrefixs,
            Map<Long, Object> instantiatedPluginMap,
            fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... pPlgParameters) {

        // Inject value
        ReflectionUtils.makeAccessible(pField);

        // Try to get a primitive type for the current parameter
        final Optional<PrimitiveObject> typeWrapper = isAPrimitiveType(pField);

        if (typeWrapper.isPresent()) {
            // The parameter is a primitive type
            LOGGER.debug(String.format("primitive parameter : %s --> %s", pField.getName(), pField.getType()));
            postProcessPrimitiveType(pPluginInstance, pPlgConf, pField, typeWrapper, pPlgParamAnnotation,
                                     pPlgParameters);
        } else {
            if (isAnInterface(pField, pPrefixs)) {
                // The wrapper is an interface plugin type
                LOGGER.debug(String.format("interface parameter : %s --> %s", pField.getName(), pField.getType()));
                postProcessInterface(pPluginInstance, pPlgConf, pField, pPlgParamAnnotation, instantiatedPluginMap);
            } else {
                throw new PluginUtilsRuntimeException(String.format("Type parameter <%s> is unknown.", pField));
            }
        }

    }

    /**
     * Use configured values to set field values for a parameter of type {@link PrimitiveObject}
     *
     * @param <T> a plugin type
     * @param pPluginInstance the plugin instance
     * @param pPlgConf the plugin configuration to used
     * @param pField the parameter
     * @param pTypeWrapper the type wrapper of the parameter
     * @param pPlgParamAnnotation the plugin parameter
     * @param pPlgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter} @ if any error occurs
     */
    private static <T> void postProcessPrimitiveType(T pPluginInstance, PluginConfiguration pPlgConf, Field pField,
            final Optional<PrimitiveObject> pTypeWrapper, PluginParameter pPlgParamAnnotation,
            fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... pPlgParameters) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting postProcessPrimitiveType :" + pPlgParamAnnotation.name());
        }

        // Get setup value
        String paramValue = pPlgConf.getParameterValue(pPlgParamAnnotation.name());

        if (pPlgParameters != null) {
            /*
             * Test if a specific value is given for this annotation parameter
             */
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> aDynamicPlgParam = Arrays
                    .asList(pPlgParameters).stream().filter(s -> s.getName().equals(pPlgParamAnnotation.name()))
                    .findFirst();
            if (aDynamicPlgParam.isPresent()) {
                /*
                 * Test if this parameter is set as dynamic in the plugin configuration
                 */
                final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> cfd = pPlgConf
                        .getParameters().stream()
                        .filter(s -> s.getName().equals(aDynamicPlgParam.get().getName()) && s.isDynamic()).findFirst();
                if (cfd.isPresent()) {
                    paramValue = postProcessDynamicValues(paramValue, cfd, aDynamicPlgParam);
                }
            }
        }

        // If the parameter value is not defined, get the default parameter value
        if (Strings.isNullOrEmpty(paramValue)) {
            paramValue = pPlgParamAnnotation.defaultValue();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("primitive parameter value : %s", paramValue));
        }

        try {
            Object effectiveVal;
            if (pTypeWrapper.get().getType().equals(String.class)) {
                effectiveVal = paramValue;
            } else {
                final Method method = pTypeWrapper.get().getType().getDeclaredMethod("valueOf", String.class);
                effectiveVal = method.invoke(null, paramValue);
            }
            pField.set(pPluginInstance, effectiveVal);
        } catch (final IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(
                    String.format("Exception while processing param <%s> in plugin class <%s> with value <%s>.",
                                  pPlgParamAnnotation.name(), pPluginInstance.getClass(), paramValue), e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ending postProcessPrimitiveType :" + pPlgParamAnnotation.name());
        }
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PluginParameter}
     *
     * @param <T> a plugin type
     * @param pPluginInstance the plugin instance
     * @param pPlgConf the plugin configuration to used
     * @param pField the parameter
     * @param pPlgParamAnnotation the plugin parameter
     * @param instantiatedPluginMap a Map of all already instantiated plugins
     * any error occurs
     */
    private static <T> void postProcessInterface(T pPluginInstance, PluginConfiguration pPlgConf, Field pField,
            PluginParameter pPlgParamAnnotation, Map<Long, Object> instantiatedPluginMap) {
        LOGGER.debug("Starting postProcessInterface :" + pPlgParamAnnotation.name());

        // Get setup value
        final PluginConfiguration paramValue = pPlgConf.getParameterConfiguration(pPlgParamAnnotation.name());

        LOGGER.debug(String.format("interface parameter value : %s", paramValue));

        try {
            // Retrieve instantated plugin from cache map
            Object effectiveVal = instantiatedPluginMap.get(paramValue.getId());
            pField.set(pPluginInstance, effectiveVal);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(
                    String.format("Exception while processing param <%s> in plugin class <%s> with value <%s>.",
                                  pPlgParamAnnotation.name(), pPluginInstance.getClass(), paramValue), e);
        }
        LOGGER.debug("Ending postProcessInterface :" + pPlgParamAnnotation.name());
    }

    /**
     * Apply a dynamic parameter value to a parameter plugin
     *
     * @param pParamValue the current parameter value
     * @param pConfiguredPlgParam the plugin parameter configured
     * @param pDynamicPlgParam the dynamic parameter value
     * @return the new parameter value @ if any error occurs
     */
    private static String postProcessDynamicValues(final String pParamValue,
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> pConfiguredPlgParam,
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> pDynamicPlgParam) {
        LOGGER.debug(String.format("Starting postProcessDynamicValues : %s - init value= <%s>",
                                   pDynamicPlgParam.get().getName(), pParamValue));

        if ((pConfiguredPlgParam.get().getDynamicsValues() != null) && (!pConfiguredPlgParam.get().getDynamicsValues()
                .isEmpty()) && (!pConfiguredPlgParam.get().getDynamicsValuesAsString()
                .contains(pDynamicPlgParam.get().getValue()))) {
            // The dynamic parameter value is not a possible value
            throw new PluginUtilsRuntimeException(
                    String.format("The dynamic value <%s> is not an authorized value for the parameter %s.",
                                  pDynamicPlgParam.get().getValue(), pDynamicPlgParam.get().getName()));
        }

        final String paramValue = pDynamicPlgParam.get().getValue();

        LOGGER.debug(String.format("Ending postProcessDynamicValues : %s - new value= <%s>",
                                   pDynamicPlgParam.get().getName(), paramValue));

        return paramValue;
    }
}
