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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterUtils.class);

    /**
     * Gson object used to deserialize configuration parameters
     */
    private static final Gson gson = new GsonBuilder().setDateFormat(DATE_TIME_FORMAT).create();

    /**
     * Default constructor
     */
    private PluginParameterUtils() {

    }

    /**
     * Retrieve {@link List} of {@link PluginParameterType} by reflection on class fields
     *
     * @param pluginClass the target {@link Class}
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param usePluginParameterAnnotation
     * @param hierarchicalParentUsedTypes
     * @return {@link List} of {@link PluginParameterType} or null
     */
    public static List<PluginParameterType> getParameters(final Class<?> pluginClass, final List<String> prefixs,
            boolean usePluginParameterAnnotation, List<String> hierarchicalParentUsedTypes) {
        List<PluginParameterType> parameters = null;

        for (final Field field : pluginClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class) || (!usePluginParameterAnnotation && isToBeConsidered(
                    field))) {
                if (parameters == null) {
                    parameters = new ArrayList<>();
                }
                ArrayList<String> parentUsedTypes = new ArrayList<String>();
                parentUsedTypes.addAll(hierarchicalParentUsedTypes);
                parameters.add(buildPluginParameter(field, prefixs, parentUsedTypes));
            }
        }
        return parameters;
    }

    /**
     * Help method allowing us to determine if the field should be taken into account or not.
     * @param field to examine
     * @return true if the field is neither final or static
     */
    private static boolean isToBeConsidered(Field field) {
        return !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers());
    }

    /**
     * Create a new {@link PluginParameterType} from a class {@link Field}
     *
     * @param field {@link Field} to get type from
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param hierarchicalParentUsedTypes already used parent types (to avoid cyclic parameters types)
     * @return {@link PluginParameterType}
     */
    private static PluginParameterType buildPluginParameter(Field field, final List<String> prefixs,
            List<String> hierarchicalParentUsedTypes) {
        PluginParameterType result;

        // Retrieve global type of Field
        ParamType fieldType = getFieldParameterType(field, prefixs);

        if (fieldType != ParamType.PRIMITIVE) {
            if (hierarchicalParentUsedTypes.contains(field.getType().getName())) {
                LOGGER.warn("Cyclic parameter types detected !! for {}", field.getType().getName());
                throw new PluginUtilsRuntimeException(String.format("Cyclic parameter types detected !! for %s",
                                                                    field.getType().getName()));
            } else {
                if (fieldType == ParamType.COLLECTION) {
                    // field is a java.util.Collection so it is a ParameterizedType which has only 1 parameter, so lets get its name
                    String parameteredTypeName = ((ParameterizedType) field.getGenericType())
                            .getActualTypeArguments()[0].getTypeName();
                    // now that we have the parametered type, only add it to the parents if it is not primitive.
                    Class<?> parameteredClass = null;
                    try {
                        parameteredClass = Class.forName(parameteredTypeName);
                    } catch (ClassNotFoundException e) {
                        PluginUtilsRuntimeException ex = new PluginUtilsRuntimeException(e.getMessage(), e);
                        LOGGER.error(ex.getMessage());
                        throw ex;
                    }
                    if (!isAPrimitiveType(parameteredClass).isPresent()) {
                        hierarchicalParentUsedTypes.add(parameteredTypeName);
                    }
                } else {
                    hierarchicalParentUsedTypes.add(field.getType().getName());
                }
            }
        }
        // Retrieve annotation if any
        final PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);

        // Create PluginParameter
        if (pluginParameter == null) {
            result = new PluginParameterType(field.getName(), field.getType().getName(), null, true, fieldType);
        } else {
            result = new PluginParameterType(pluginParameter.name(),
                                             field.getType().getName(),
                                             pluginParameter.defaultValue(),
                                             pluginParameter.optional(),
                                             fieldType);
            // now that we get a PluginParameterType for generic case, lets update information according to what the dev provided
            if (pluginParameter.paramType() != ParamType.UNDEFINED) {
                result.setParamType(pluginParameter.paramType());
            }
            if (!void.class.equals(pluginParameter.type())) {
                result.setType(pluginParameter.type().getName());
            }
        }

        // If the Field is OBJECT or PARAMETERIZED_OBJECT, then add associated PluginParameter for each object field.
        if ((fieldType == ParamType.OBJECT) || (fieldType == ParamType.COLLECTION)) {
            Class<?> classType;
            if (field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                classType = (Class<?>) type.getActualTypeArguments()[0];
            } else {
                classType = field.getType();
            }
            // Set the parameterized subtype
            result.setParameterizedSubType(classType.getName());
            if (!isAPrimitiveType(classType).isPresent()) {
                List<PluginParameterType> list = getParameters(classType, prefixs, false, hierarchicalParentUsedTypes);
                result.setParameters(list);
            }
        }

        return result;
    }

    /**
     * Retrieve the {@link ParamType} associated to the given {@link Field}
     * @param field {@link Field} to get type from
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return
     */
    private static ParamType getFieldParameterType(Field field, final List<String> prefixs) {
        ParamType parameterType;
        if (isAPrimitiveType(field).isPresent()) {
            parameterType = ParamType.PRIMITIVE;
        } else if (isAnInterface(field, prefixs)) {
            parameterType = ParamType.PLUGIN;
        } else if (field.getGenericType() instanceof ParameterizedType) {
            // FIXME : Handle for multi parameterized object
            parameterType = ParamType.COLLECTION;
        } else {
            parameterType = ParamType.OBJECT;
        }
        return parameterType;
    }

    /**
     * Search a field in the {@link PrimitiveObject}
     *
     * @param field {@link Field} to get type from
     * @return an {@link Optional} {@link PrimitiveObject}
     */
    private static Optional<PrimitiveObject> isAPrimitiveType(final Field field) {
        return Arrays.asList(PrimitiveObject.values()).stream()
                .filter(s -> field.getType().isAssignableFrom(s.getType())).findFirst();
    }

    /**
     * Search a field in the {@link PrimitiveObject}
     *
     * @param jClass a {@link Class}
     * @return an {@link Optional} {@link PrimitiveObject}
     */
    private static Optional<PrimitiveObject> isAPrimitiveType(final Class<?> jClass) {
        return Arrays.asList(PrimitiveObject.values()).stream().filter(s -> jClass.isAssignableFrom(s.getType()))
                .findFirst();
    }

    /**
     * Search a field like a {@link PluginInterface}
     *
     * @param field {@link Field} to get type from
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return true is the type of the field is a {@link Plugin}
     */
    private static boolean isAnInterface(final Field field, final List<String> prefixs) {
        boolean isInterface = false;
        final ListIterator<String> listIter = prefixs.listIterator();

        while (listIter.hasNext() && !isInterface) {
            final String s = listIter.next();
            isInterface = isAnInterface(field, s);
        }

        return isInterface;
    }

    /**
     * Search a field like a {@link PluginInterface}
     *
     * @param field {@link Field} to get type from
     * @param prefix a package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return true is the type of the field is a {@link PrimitiveObject}
     */
    private static boolean isAnInterface(final Field field, final String prefix) {
        boolean isSupportedType = false;
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(prefix);

        if ((pluginInterfaces != null) && !pluginInterfaces.isEmpty()) {
            isSupportedType =
                    pluginInterfaces.stream().filter(s -> s.equalsIgnoreCase(field.getType().getName())).count() > 0;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("interface parameter : %s --> %b", field.toGenericString(), isSupportedType));
        }

        return isSupportedType;
    }

    /**
     * Use configured values to set field values.
     *
     * @param @param <T> a {@link Plugin} type
     * @param returnPlugin the plugin instance
     * @param plgConf the {@link PluginConfiguration}
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param instantiatedPluginMap a {@link Map} of already instantiated {@link Plugin}
     * @param plgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter} @ if any error occurs
     */
    public static <T> void postProcess(final T returnPlugin, final PluginConfiguration plgConf,
            final List<String> prefixs, Map<Long, Object> instantiatedPluginMap,
            final fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... plgParameters) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting postProcess :" + returnPlugin.getClass().getSimpleName());
        }

        // Test if the plugin configuration is active
        if (!plgConf.isActive()) {
            throw new PluginUtilsRuntimeException(String.format("The plugin configuration <%s-%s> is not active.",
                                                                plgConf.getId(),
                                                                plgConf.getLabel()));
        }

        // Look for annotated fields
        for (final Field field : returnPlugin.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                final PluginParameter plgParamAnnotation = field.getAnnotation(PluginParameter.class);
                processPluginParameter(returnPlugin,
                                       plgConf,
                                       field,
                                       plgParamAnnotation,
                                       prefixs,
                                       instantiatedPluginMap,
                                       plgParameters);
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ending postProcess :" + returnPlugin.getClass().getSimpleName());
        }
    }

    /**
     * Use configured values to set field value for a {@link PluginParameter}
     *
     * @param <T> a {@link Plugin} type
     * @param pluginInstance the {@link Plugin} instance
     * @param plgConf the plugin configuration to used
     * @param field the parameter
     * @param plgParamAnnotation the {@link PluginParameter} annotation
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param instantiatedPluginMap a {@link Map} of already instantiated {@link Plugin}
     * @param plgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter} @ if any error occurs
     */
    private static <T> void processPluginParameter(T pluginInstance, PluginConfiguration plgConf, Field field,
            PluginParameter plgParamAnnotation, List<String> prefixs, Map<Long, Object> instantiatedPluginMap,
            fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... plgParameters) {

        // Inject value
        ReflectionUtils.makeAccessible(field);

        // Try to get a primitive type for the current parameter
        final Optional<PrimitiveObject> typeWrapper = isAPrimitiveType(field);

        switch (getFieldParameterType(field, prefixs)) {
            case PRIMITIVE:
                LOGGER.debug(String.format("primitive parameter : %s --> %s", field.getName(), field.getType()));
                postProcessPrimitiveType(pluginInstance,
                                         plgConf,
                                         field,
                                         typeWrapper,
                                         plgParamAnnotation,
                                         plgParameters);
                break;
            case PLUGIN:
                LOGGER.debug(String.format("interface parameter : %s --> %s", field.getName(), field.getType()));
                postProcessInterface(pluginInstance, plgConf, field, plgParamAnnotation, instantiatedPluginMap);
                break;
            case OBJECT:
                LOGGER.debug(String.format("Object parameter : %s --> %s", field.getName(), field.getType()));
                postProcessObjectType(pluginInstance, plgConf, field, plgParamAnnotation, false, plgParameters);
                break;
            case COLLECTION:
                LOGGER.debug(String.format("Object parameter : %s --> %s", field.getName(), field.getType()));
                postProcessObjectType(pluginInstance, plgConf, field, plgParamAnnotation, true, plgParameters);
                break;
            default:
                throw new PluginUtilsRuntimeException(String.format("Type parameter <%s> is unknown.", field));
        }
    }

    /**
     *  Use configured values to set field values for a parameter of type OBJECT or PARAMETERIZED_OBJECT
     *
     * @param <T> a {@link Plugin} type
     * @param pluginInstance the {@link Plugin} instance
     * @param plgConf the plugin configuration to used
     * @param field the parameter
     * @param plgParamAnnotation the {@link PluginParameter} annotation
     * @param isCollection does the parameter is a Collection ?
     * @param plgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter}
     */
    private static <T, V> void postProcessObjectType(T pluginInstance, PluginConfiguration plgConf, Field field,
            PluginParameter plgParamAnnotation, boolean isCollection,
            fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... plgParameters) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting postProcessObjectType :" + plgParamAnnotation.name());
        }

        if (field.getType().isInterface() && !isCollection) {
            throw new PluginUtilsRuntimeException(String.format(
                    "Invalid plugin parameter of non instanciable interface %s",
                    field.getType().getName()));
        }
        if (isCollection && !Collection.class.isAssignableFrom(field.getType())) {
            throw new PluginUtilsRuntimeException(String.format(
                    "Invalid plugin parameter: plugin parameter %s is supposed to be of type %s but is not. It is of type : %s",
                    field.getName(),
                    Collection.class.getName(),
                    field.getType().getName()));
        }

        // Get setup value
        String paramValue = plgConf.getParameterValue(plgParamAnnotation.name());

        if (plgParameters != null) {
            /*
             * Test if a specific value is given for this annotation parameter
             */
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> aDynamicPlgParam = Arrays
                    .asList(plgParameters).stream().filter(s -> s.getName().equals(plgParamAnnotation.name()))
                    .findFirst();
            if (aDynamicPlgParam.isPresent()) {
                /*
                 * Test if this parameter is set as dynamic in the plugin configuration
                 */
                final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> cfd = plgConf
                        .getParameters().stream()
                        .filter(s -> s.getName().equals(aDynamicPlgParam.get().getName()) && s.isDynamic()).findFirst();
                if (cfd.isPresent()) {
                    paramValue = postProcessDynamicValues(paramValue, cfd, aDynamicPlgParam);
                }
            }
        }

        // Do not handle default value for object types.
        if (paramValue != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Object parameter json value : %s", paramValue));
            }
            // Deserialize object from JSON value
            Type paramType = field.getType();
            // Deserialization of parameteried type can be tricky, lets try to help gson here
            if (isCollection && plgParamAnnotation.type() != void.class) {
                // if type of the object has been specified by the dev on the parameter, lets use it
                paramType = new TypeToken<Collection<V>>() {

                }.where(new TypeParameter<V>() {

                }, TypeToken.of(plgParamAnnotation.type())).getType();
            }
            Object objectParamValue = gson.fromJson(paramValue, paramType);

            if (objectParamValue != null) {
                try {
                    field.set(pluginInstance, objectParamValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    LOGGER.error(String.format("Error during Object parameter deserialization for parameter %s",
                                               field.getName()), e);
                }
            }
        }
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PrimitiveObject}
     *
     * @param <T> a {@link Plugin} type
     * @param pluginInstance the {@link Plugin} instance
     * @param plgConf the {@link PluginConfiguration} to used
     * @param field the parameter
     * @param typeWrapper the type wrapper of the parameter
     * @param plgParamAnnotation the {@link PluginParameter} annotation
     * @param plgParameters an optional set of
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginParameter} @ if any error occurs
     */
    private static <T> void postProcessPrimitiveType(T pPluginInstance, PluginConfiguration plgConf, Field field,
            final Optional<PrimitiveObject> typeWrapper, PluginParameter plgParamAnnotation,
            fr.cnes.regards.framework.modules.plugins.domain.PluginParameter... plgParameters) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting postProcessPrimitiveType :" + plgParamAnnotation.name());
        }

        // Get setup value
        String paramValue = plgConf.getParameterValue(plgParamAnnotation.name());

        if (plgParameters != null) {
            /*
             * Test if a specific value is given for this annotation parameter
             */
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> aDynamicPlgParam = Arrays
                    .asList(plgParameters).stream().filter(s -> s.getName().equals(plgParamAnnotation.name()))
                    .findFirst();
            if (aDynamicPlgParam.isPresent()) {
                /*
                 * Test if this parameter is set as dynamic in the plugin configuration
                 */
                final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> cfd = plgConf
                        .getParameters().stream()
                        .filter(s -> s.getName().equals(aDynamicPlgParam.get().getName()) && s.isDynamic()).findFirst();
                if (cfd.isPresent()) {
                    paramValue = postProcessDynamicValues(paramValue, cfd, aDynamicPlgParam);
                }
            }
        }

        // If the parameter value is not defined, get the default parameter value
        if (Strings.isNullOrEmpty(paramValue)) {
            paramValue = plgParamAnnotation.defaultValue();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("primitive parameter value : %s", paramValue));
        }

        try {
            Object effectiveVal;
            if (typeWrapper.get().getType().equals(PrimitiveObject.STRING.getType())) {
                effectiveVal = paramValue;
            } else {
                final Method method = typeWrapper.get().getType().getDeclaredMethod("valueOf", String.class);
                effectiveVal = method.invoke(null, paramValue);
            }
            field.set(pPluginInstance, effectiveVal);
        } catch (final IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(String.format(
                    "Exception while processing param <%s> in plugin class <%s> with value <%s>.",
                    plgParamAnnotation.name(),
                    pPluginInstance.getClass(),
                    paramValue), e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ending postProcessPrimitiveType :" + plgParamAnnotation.name());
        }
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PluginParameter}
     *
     * @param <T> a {@link Plugin} type
     * @param pluginInstance the {@link Plugin} instance
     * @param plgConf the plugin configuration to used
     * @param field the parameter
     * @param plgParamAnnotation the {@link PluginParameter} annotation
     * @param instantiatedPluginMap a Map of all already instantiated plugins
     */
    private static <T> void postProcessInterface(T pluginInstance, PluginConfiguration plgConf, Field field,
            PluginParameter plgParamAnnotation, Map<Long, Object> instantiatedPluginMap) {
        LOGGER.debug("Starting postProcessInterface :" + plgParamAnnotation.name());

        // Get setup value
        final PluginConfiguration paramValue = plgConf.getParameterConfiguration(plgParamAnnotation.name());

        LOGGER.debug(String.format("interface parameter value : %s", paramValue));

        try {
            // Retrieve instantiated plugin from cache map
            Object effectiveVal = instantiatedPluginMap.get(paramValue.getId());
            field.set(pluginInstance, effectiveVal);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(String.format(
                    "Exception while processing param <%s> in plugin class <%s> with value <%s>.",
                    plgParamAnnotation.name(),
                    pluginInstance.getClass(),
                    paramValue), e);
        }
        LOGGER.debug("Ending postProcessInterface :" + plgParamAnnotation.name());
    }

    /**
     * Apply a dynamic parameter value to a parameter plugin
     *
     * @param paramValue the current parameter value
     * @param configuredPlgParam the plugin parameter configured
     * @param dynamicPlgParam the dynamic parameter value
     * @return the new parameter value
     */
    private static String postProcessDynamicValues(final String paramValue,
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> configuredPlgParam,
            final Optional<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> dynamicPlgParam) {
        LOGGER.debug(String.format("Starting postProcessDynamicValues : %s - init value= <%s>",
                                   dynamicPlgParam.get().getName(),
                                   paramValue));

        if ((configuredPlgParam.get().getDynamicsValues() != null) && (!configuredPlgParam.get().getDynamicsValues()
                .isEmpty()) && (!configuredPlgParam.get().getDynamicsValuesAsString()
                .contains(dynamicPlgParam.get().getValue()))) {
            // The dynamic parameter value is not a possible value
            throw new PluginUtilsRuntimeException(String.format(
                    "The dynamic value <%s> is not an authorized value for the parameter %s.",
                    dynamicPlgParam.get().getValue(),
                    dynamicPlgParam.get().getName()));
        }

        final String newValue = dynamicPlgParam.get().getValue();

        LOGGER.debug(String.format("Ending postProcessDynamicValues : %s - new value= <%s>",
                                   dynamicPlgParam.get().getName(),
                                   newValue));

        return newValue;
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
        BOOLEAN(Boolean.class),

        SET(Set.class);

        /**
         * Type
         */
        private final Class<?> type;

        /**
         * Constructor
         *
         * @param pType primitive type
         */
        private PrimitiveObject(final Class<?> pType) {
            type = pType;
        }

        /**
         * Get method.
         *
         * @return the type
         */
        public Class<?> getType() {
            return type;
        }
    }
}
