/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.AbstractPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.BooleanPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.BytePluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.DoublePluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.FloatPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IntegerPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.LongPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.NestedPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.PluginParamType;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.ShortPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.StringPluginParam;

/**
 * Post process plugin instances to inject annotated parameters.
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public final class PluginParameterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterUtils.class);

    private static final String SKIPPING_VALUE_INJECTION_FOR_OPTIONAL = "Skipping value injection for optional parameter {}. No param configured!";

    private static final String PARAMETER_NOT_FOUND_IN_PLUGIN_CONFIGURATION = "Issue with Plugin %s and one of its configuration %s, parameter %s not found.";

    private static final String NO_VALUE_SPECIFIED_FOR_OPTIONNAL_PARAMETER = "Skipping value injection for optional parameter {}. No value specified!";

    private static final String PLUGIN_PARAMETER_INJECTED = "Plugin parameter \"{}\" injected!";

    private static final String NO_PLUGIN_PARAMETER_VALUE_AND_IS_REQUIRED = "Issue with Plugin %s and one of its configuration %s, parameter %s has no default value and is required.";

    private static final String EXCEPTION_WHILE_PROCESSING_PARAM_IN_PLUGIN = "Exception while processing param <%s> in plugin class <%s> with param <%s>.";

    /**
     * Retrieve List of {@link PluginParamDescriptor} by reflection on class fields
     * @param pluginClass the target class
     * @return List of {@link PluginParamDescriptor} or null.
     */
    public static List<PluginParamDescriptor> getParameters(Class<?> pluginClass) {
        return getParameters(pluginClass, false, null);
    }

    /**
     * Retrieve {@link List} of {@link PluginParamDescriptor} by reflection on class fields with in depth parameter
     * discovery
     * @param pluginClass the plugin implementation
     * @param isChildParameters if <code>true</code>, {@link PluginParameter} is not required for in depth parameters
     * discovery.
     * @param alreadyManagedTypeNames List of already managed type names to detect cyclic references during in depth
     * parameters discovery.
     * @return list of {@link PluginParamDescriptor} or null
     */
    private static List<PluginParamDescriptor> getParameters(Class<?> pluginClass, boolean isChildParameters,
            List<String> alreadyManagedTypeNames) {
        List<PluginParamDescriptor> parameters = new ArrayList<>();

        for (final Field field : ReflectionUtils.getAllDeclaredFields(pluginClass)) {
            if (field.isAnnotationPresent(PluginParameter.class) || (isChildParameters && isToBeConsidered(field))) {
                // Initialize list of managed types for in depth scanning from root fields
                List<String> managedTypes = new ArrayList<>();
                if (isChildParameters) {
                    managedTypes.addAll(alreadyManagedTypeNames);
                }
                parameters.add(buildPluginParameter(field, pluginClass, managedTypes));
            }
        }
        return parameters.isEmpty() ? null : parameters;
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
     * Create a new {@link PluginParamDescriptor} from a class {@link Field}
     * @param field {@link Field} to get type from
     * @param pluginClass the plugin implementation
     * @param alreadyManagedTypeNames List of already managed type names to detect cyclic references during in depth
     * parameters discovery.
     * @return {@link PluginParamDescriptor}
     */
    private static PluginParamDescriptor buildPluginParameter(Field field, Class<?> pluginClass,
            List<String> alreadyManagedTypeNames) {
        PluginParamDescriptor result;

        // Retrieve type of field
        PluginParamType paramType = getFieldParameterType(field);
        String pluginType = null;

        if (paramType == PluginParamType.PLUGIN) {
            pluginType = field.getType().getName();
        }

        // Retrieve annotation if any
        PluginParameter pluginParameter = field.getAnnotation(PluginParameter.class);

        // Create PluginParameter
        if (pluginParameter == null) {
            // Guess values
            result = PluginParamDescriptor.create(field.getName(), field.getName(), null, paramType, false, false,
                                                  false, pluginType);
        } else {
            // Report values from annotation
            String name = getFieldName(field, pluginParameter);

            // Lets restrict @PluginParameter#sensitive() usage to strings.
            if (pluginParameter.sensitive()) {
                if (!String.class.isAssignableFrom(field.getType())) {
                    String msg = String
                            .format("Sensible parameters must be of type %s. Faulty parameter: %s in plugin: %s",
                                    String.class.getName(), field.getName(), pluginClass.getName());
                    LOGGER.error(msg);
                    throw new PluginUtilsRuntimeException(msg);
                }
            }

            result = PluginParamDescriptor.create(name, pluginParameter.label(), pluginParameter.description(),
                                                  paramType, pluginParameter.optional(),
                                                  pluginParameter.unconfigurable(), pluginParameter.sensitive(),
                                                  pluginType);

            // Manage markdown description
            String markdown = AnnotationUtils.loadMarkdown(pluginClass, pluginParameter.markdown());
            if ((markdown != null) && !markdown.isEmpty()) {
                result.setMarkdown(markdown);
            }

            // Manage default value
            if ((pluginParameter.defaultValue() != null) && !pluginParameter.defaultValue().isEmpty()) {
                result.setDefaultValue(pluginParameter.defaultValue());
            }
        }

        // Do in depth discovery for OBJECT
        // Note : at the moment an OBJECT cannot be a PARAMETERIZED type : {@link #getFieldParameterType(Field, List)}
        if (PluginParamType.POJO.equals(paramType)) {
            // Register current type in already managed ones
            registerTypeName(alreadyManagedTypeNames, field.getType());
            // Propagate discovery
            result.addAllParameters(getParameters(field.getType(), true, alreadyManagedTypeNames));
        }

        // Do in depth discovery for COLLECTION and register parameterized sub type
        else if (PluginParamType.COLLECTION.equals(paramType)) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            // Get single parameter type
            Class<?> argType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            // Propagate discovery
            PluginParamType subParamType = tryPropagatingDiscovery(field.getType(), argType, alreadyManagedTypeNames,
                                                                   result);
            result.setParameterizedSubTypes(subParamType);
        }

        // Do in depth discovery for MAP and register parameterized sub types
        else if (PluginParamType.MAP.equals(paramType)) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            // Set key label
            if (pluginParameter != null) {
                result.setKeyLabel(pluginParameter.keylabel());
            }
            // Get parameter types
            Class<?> keyType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            Class<?> valueType = (Class<?>) parameterizedType.getActualTypeArguments()[1];
            // Propagate discovery
            PluginParamType subParamType0 = tryPropagatingDiscovery(field.getType(), keyType, alreadyManagedTypeNames,
                                                                    result);
            PluginParamType subParamType1 = tryPropagatingDiscovery(field.getType(), valueType, alreadyManagedTypeNames,
                                                                    result);
            result.setParameterizedSubTypes(subParamType0, subParamType1);
        }
        return result;
    }

    /**
     * Get configuration parameter name.
     * @param field {@link Field}
     * @param pluginParameter {@link PluginParameter} field annotation
     * @return the parameter's name used as a key for database registration. If not specified, falling back to field
     * name.
     */
    private static String getFieldName(Field field, PluginParameter pluginParameter) {
        return pluginParameter.name().isEmpty() ? field.getName() : pluginParameter.name();
    }

    /**
     * Try propagating parameter discovery to parameterized type arguments. May throw a
     * {@link PluginUtilsRuntimeException} if an argument is a {@link Collection} or {@link Map}.
     * @param rawType raw type
     * @param argType one argument type
     * @param alreadyManagedTypeNames List of already managed type names to detect cyclic references during in depth
     * parameters discovery
     * @param result current {@link PluginParamDescriptor}
     */
    private static PluginParamType tryPropagatingDiscovery(Class<?> rawType, Class<?> argType,
            List<String> alreadyManagedTypeNames, PluginParamDescriptor result) {

        PluginParamType paramType;
        Optional<PrimitiveObject> po = findPrimitiveObject(argType);
        if (po.isPresent()) {
            LOGGER.debug("Primitive type detected. Stop plugin parameters discovery.");
            paramType = po.get().getParamType();
        } else if (isAnInterface(argType)) {
            LOGGER.debug("Plugin type detected. Stop plugin parameters discovery.");
            paramType = PluginParamType.PLUGIN;
        } else if (Collection.class.isAssignableFrom(argType) || Map.class.isAssignableFrom(argType)) {
            String message = String.format("Parameterized argument of type collection or map is not supported : %s",
                                           rawType.getName());
            LOGGER.error(message);
            throw new PluginUtilsRuntimeException(message);
        } else {
            // Register current type in already managed ones
            registerTypeName(alreadyManagedTypeNames, argType);
            // Propagate discovery
            result.addAllParameters(getParameters(argType, true, alreadyManagedTypeNames));
            paramType = PluginParamType.POJO;
        }
        return paramType;
    }

    /**
     * Check if clazz not already managed and add it to the list if not
     * @param alreadyManagedTypeNames already managed type names
     * @param clazz class to check
     */
    private static void registerTypeName(List<String> alreadyManagedTypeNames, Class<?> clazz) {
        if (alreadyManagedTypeNames.contains(clazz.getName())) {
            String message = String.format("Cyclic parameter types detected !! for %s", clazz.getName());
            LOGGER.error(message);
            throw new PluginUtilsRuntimeException(message);
        } else {
            alreadyManagedTypeNames.add(clazz.getName());
        }
    }

    /**
     * Retrieve the {@link PluginParamType} associated to the given {@link Field}
     * @param field {@link Field} to get type from
     */
    private static PluginParamType getFieldParameterType(Field field) {

        Optional<PrimitiveObject> o = findPrimitiveObject(field.getType());
        if (o.isPresent()) {
            return o.get().getParamType();
        }

        if (isAnInterface(field.getType())) {
            return PluginParamType.PLUGIN;
        }

        if (field.getGenericType() instanceof ParameterizedType) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                return PluginParamType.COLLECTION;
            } else if (Map.class.isAssignableFrom(field.getType())) {
                return PluginParamType.MAP;
            } else {
                throw new PluginUtilsRuntimeException(
                        String.format("Parameter type not supported : %s", field.getGenericType()));
            }
        }

        // Else
        return PluginParamType.POJO;
    }

    /**
     * Search a clazz in the {@link PrimitiveObject}
     * @param clazz class to analyze
     * @return an {@link Optional} {@link PrimitiveObject}
     */
    private static Optional<PrimitiveObject> findPrimitiveObject(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            for (PrimitiveObject po : PrimitiveObject.values()) {
                if (clazz.isAssignableFrom(po.getPrimitive())) {
                    return Optional.of(po);
                }
            }
        } else {
            for (PrimitiveObject po : PrimitiveObject.values()) {
                if (clazz.isAssignableFrom(po.getType())) {
                    return Optional.of(po);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Search a field like a {@link PluginInterface}
     * @param clazz class to analyse
     * @return true is the type of the field is a {@link PrimitiveObject}
     */
    private static boolean isAnInterface(Class<?> clazz) {
        boolean isSupportedType = false;
        Set<String> pluginInterfaces = PluginUtils.getPluginInterfaces();

        if ((pluginInterfaces != null) && !pluginInterfaces.isEmpty()) {
            isSupportedType = pluginInterfaces.stream().anyMatch(s -> s.equalsIgnoreCase(clazz.getName()));
        }

        LOGGER.debug("interface parameter : {} --> {}", clazz.getName(), isSupportedType);

        return isSupportedType;
    }

    /**
     * Use configured values to set field values.
     * @param <T> a {@link Plugin} type
     * @param plugin the plugin instance
     * @param conf the {@link PluginConfiguration}
     * @param instantiatedPlugins a {@link Map} of already instantiated {@link Plugin}
     * @param dynamicParams an optional set of {@link IPluginParam}
     */
    public static <T> void postProcess(T plugin, PluginConfiguration conf, Map<String, Object> instantiatedPlugins,
            IPluginParam... dynamicParams) {

        LOGGER.debug("Post processing plugin \"{}\"", plugin.getClass().getSimpleName());

        // Test if the plugin configuration is active
        if (!conf.isActive()) {
            throw new PluginUtilsRuntimeException(
                    String.format("The plugin configuration <%s-%s> is not active.", conf.getId(), conf.getLabel()));
        }

        // Look for annotated fields
        for (Field field : ReflectionUtils.getAllDeclaredFields(plugin.getClass())) {
            if (field.isAnnotationPresent(PluginParameter.class)) {
                PluginParameter plgParamAnnotation = field.getAnnotation(PluginParameter.class);
                processPluginParameter(plugin, conf, field, plgParamAnnotation, instantiatedPlugins, dynamicParams);
            }
        }

        LOGGER.debug("Post processing successful for plugin \"{}\"", plugin.getClass().getSimpleName());
    }

    /**
     * Use configured values to set field value for a {@link PluginParameter}
     * @param <T> a {@link Plugin} type
     * @param plugin the {@link Plugin} instance
     * @param conf the plugin configuration to used
     * @param field the parameter
     * @param paramAnnotation the {@link PluginParameter} annotation
     * @param instantiatedPlugins a {@link Map} of already instantiated {@link Plugin}
     * @param dynamicParams an optional set of {@link IPluginParam}
     */
    private static <T> void processPluginParameter(T plugin, PluginConfiguration conf, Field field,
            PluginParameter paramAnnotation, Map<String, Object> instantiatedPlugins, IPluginParam... dynamicParams) {

        // Inject value
        ReflectionUtils.makeAccessible(field);

        PluginParamType paramType = getFieldParameterType(field);
        switch (paramType) {
            case STRING:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                LOGGER.debug("primitive parameter : {} --> {}", field.getName(), field.getType());
                postProcessPrimitiveType(plugin, conf, field, findPrimitiveObject(field.getType()).get(),
                                         paramAnnotation, dynamicParams);
                break;
            case PLUGIN:
                LOGGER.debug("interface parameter : {} --> {}", field.getName(), field.getType());
                postProcessInterface(plugin, conf, field, paramAnnotation, instantiatedPlugins);
                break;
            case POJO:
            case COLLECTION:
            case MAP:
                LOGGER.debug("object parameter {} : {} --> {}", paramType, field.getName(), field.getType());
                postProcessObjectType(plugin, conf, field, paramAnnotation, paramType, dynamicParams);
                break;
            default:
                throw new PluginUtilsRuntimeException(String.format("Type parameter <%s> is unknown.", field));
        }
    }

    /**
     * Creates a new {@link IPluginParam} from a {@link PluginParamType}
     * @param paramType
     * @param paramName
     * @param value
     * @return new created {@link IPluginParam}
     */
    public static IPluginParam forType(PluginParamType paramType, String paramName, String value, boolean isDynamic) {
        AbstractPluginParam<?> param = null;
        switch (paramType) {
            case STRING:
                param = IPluginParam.build(paramName, value);
                break;
            case BYTE:
                param = IPluginParam.build(paramName, Byte.valueOf(value));
                break;
            case SHORT:
                param = IPluginParam.build(paramName, Short.valueOf(value));
                break;
            case INTEGER:
                param = IPluginParam.build(paramName, Integer.valueOf(value));
                break;
            case LONG:
                param = IPluginParam.build(paramName, Long.valueOf(value));
                break;
            case FLOAT:
                param = IPluginParam.build(paramName, Float.valueOf(value));
                break;
            case DOUBLE:
                param = IPluginParam.build(paramName, Double.valueOf(value));
                break;
            case BOOLEAN:
                param = IPluginParam.build(paramName, Boolean.valueOf(value));
                break;
            case POJO:
            case COLLECTION:
            case MAP:
            case PLUGIN:
                // FIXME : Handle complex types
            default:
                throw new PluginUtilsRuntimeException(
                        String.format("Type parameter <%s> cannot be handled. Complex types are not supported yet.",
                                      paramType));
        }
        param.setDynamic(isDynamic);
        return param;
    }

    /**
     * Find the plugin parameter from the plugin configuration or from dynamic plugin parameters if there are
     * any.
     * @param parameterName parameter name
     * @param pluginConf plugin configuration
     * @param dynamicPluginParameters dynamic plugin parameters
     * @return {@link IPluginParam} or null
     */
    private static IPluginParam findParameter(String parameterName, PluginConfiguration pluginConf,
            IPluginParam... dynamicPluginParameters) {
        // Default value comes from plugin configuration
        IPluginParam staticParam = pluginConf.getParameter(parameterName);
        // Manage dynamic parameters
        // IF static parameter is not found, so the parameter can only be dynamic.
        if ((dynamicPluginParameters != null) && (dynamicPluginParameters.length > 0)) {
            // Search dynamic parameter for current parameter name
            Optional<IPluginParam> dynamicParameterOpt = Arrays.stream(dynamicPluginParameters)
                    .filter(s -> s.getName().equals(parameterName)).findFirst();
            // Dynamic parameter found
            if (dynamicParameterOpt.isPresent()) {
                IPluginParam dynamicParam = dynamicParameterOpt.get();
                LOGGER.debug("Dynamic parameter found for parameter \"{}\" with value \"{}\"", parameterName,
                             dynamicParam);
                if (staticParam != null) {
                    if (!staticParam.isValid(dynamicParam)) {
                        throw new PluginUtilsRuntimeException(
                                String.format("Dynamic param %s not consistent with static one %s", dynamicParam,
                                              staticParam));
                    }
                    if (!staticParam.isDynamic()) {
                        throw new PluginUtilsRuntimeException(
                                String.format("Param %s is not allowed to be dynamic", dynamicParam, staticParam));
                    }
                }
                return dynamicParam;
            }
        }
        return staticParam;
    }

    /**
     * Use configured values to set field values for a parameter of type OBJECT or PARAMETERIZED_OBJECT
     * @param <T> a {@link Plugin} type
     * @param plugin the {@link Plugin} instance
     * @param conf the plugin configuration to used
     * @param field the parameter
     * @param paramAnnotation the {@link PluginParameter} annotation
     * @param paramType param type to consider
     * @param dynamicParams an optional set of {@link IPluginParam}
     */
    private static <T> void postProcessObjectType(T plugin, PluginConfiguration conf, Field field,
            PluginParameter paramAnnotation, PluginParamType paramType, IPluginParam... dynamicParams) {

        LOGGER.debug("Injecting object plugin parameter \"{}\"", paramAnnotation.label());

        // Retrieve parameter name
        String parameterName = getFieldName(field, paramAnnotation);

        if (field.getType().isInterface()
                && !(PluginParamType.COLLECTION.equals(paramType) || PluginParamType.MAP.equals(paramType))) {
            throw new PluginUtilsRuntimeException(String
                    .format("Invalid plugin parameter of non instanciable interface %s", field.getType().getName()));
        }
        if (PluginParamType.COLLECTION.equals(paramType) && !Collection.class.isAssignableFrom(field.getType())) {
            throw new PluginUtilsRuntimeException(String
                    .format("Invalid plugin parameter: plugin parameter %s is supposed to be of type %s but is not. It is of type : %s",
                            parameterName, Collection.class.getName(), field.getType().getName()));
        }
        if (PluginParamType.MAP.equals(paramType) && !Map.class.isAssignableFrom(field.getType())) {
            throw new PluginUtilsRuntimeException(String
                    .format("Invalid plugin parameter: plugin parameter %s is supposed to be of type %s but is not. It is of type : %s",
                            parameterName, Map.class.getName(), field.getType().getName()));
        }

        // Get plugin parameter configuration
        IPluginParam param = findParameter(parameterName, conf, dynamicParams);

        if (param == null) {
            if (paramAnnotation.optional()) {
                LOGGER.debug(SKIPPING_VALUE_INJECTION_FOR_OPTIONAL, parameterName);
                return;
            } else {
                throw new IllegalArgumentException(String.format(PARAMETER_NOT_FOUND_IN_PLUGIN_CONFIGURATION,
                                                                 conf.getPluginId(), conf.getLabel(), parameterName));
            }
        }

        // Object type cannot have default value
        // Nothing to do

        // Stop if no value and optional parameter
        if (!param.hasValue() && paramAnnotation.optional()) {
            LOGGER.debug(NO_VALUE_SPECIFIED_FOR_OPTIONNAL_PARAMETER, parameterName);
            return;
        }

        // At this point, if the parameter value is not set, there is a problem
        if (!param.hasValue()) {
            throw new IllegalArgumentException(String.format(NO_PLUGIN_PARAMETER_VALUE_AND_IS_REQUIRED,
                                                             conf.getPluginId(), conf.getLabel(), parameterName));
        }

        try {
            // Tansform to real value
            Object o = PluginParameterTransformer.getParameterValue(param, field);
            field.set(plugin, o);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(String.format(EXCEPTION_WHILE_PROCESSING_PARAM_IN_PLUGIN,
                                                                paramAnnotation.label(), plugin.getClass(), param),
                    e);
        }
        LOGGER.debug(PLUGIN_PARAMETER_INJECTED, paramAnnotation.label());
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PrimitiveObject}
     * @param <T> a {@link Plugin} type
     * @param plugin the {@link Plugin} instance
     * @param conf the {@link PluginConfiguration} to used
     * @param field the parameter
     * @param typeWrapper the type wrapper of the parameter
     * @param paramAnnotation the {@link PluginParameter} annotation
     * @param dynamicParams an optional set of {@link IPluginParam}
     */
    private static <T> void postProcessPrimitiveType(T plugin, PluginConfiguration conf, Field field,
            PrimitiveObject typeWrapper, PluginParameter paramAnnotation, IPluginParam... dynamicParams) {

        LOGGER.debug("Injecting primitive plugin parameter \"{}\"", paramAnnotation.label());

        // Retrieve parameter name
        String parameterName = getFieldName(field, paramAnnotation);

        // Get plugin parameter configuration
        IPluginParam param = findParameter(parameterName, conf, dynamicParams);

        if (param == null) {
            if (paramAnnotation.optional() && paramAnnotation.defaultValue().isEmpty()) {
                LOGGER.debug(SKIPPING_VALUE_INJECTION_FOR_OPTIONAL, parameterName);
                return;
            } else if (!paramAnnotation.defaultValue().isEmpty()) {
                // Init a parameter on the fly
                param = buildByType(typeWrapper.getType(), parameterName);
            } else {
                throw new IllegalArgumentException(String.format(PARAMETER_NOT_FOUND_IN_PLUGIN_CONFIGURATION,
                                                                 conf.getPluginId(), conf.getLabel(), parameterName));
            }
        }

        // Primitive type can have default value
        if (!param.hasValue() && !paramAnnotation.defaultValue().isEmpty()) {
            if (!param.supportsDefaultValue()) {
                LOGGER.warn("Skipping default value injection for parameter {}. Default value not supported!",
                            parameterName);
            } else {
                param.applyDefaultValue(paramAnnotation.defaultValue());
            }
        }

        // Stop if no value and optional parameter
        if (!param.hasValue() && paramAnnotation.optional()) {
            LOGGER.debug(NO_VALUE_SPECIFIED_FOR_OPTIONNAL_PARAMETER, parameterName);
            return;
        }

        // At this point, if the parameter value is not set, there is a problem
        if (!param.hasValue()) {
            throw new IllegalArgumentException(String.format(NO_PLUGIN_PARAMETER_VALUE_AND_IS_REQUIRED,
                                                             conf.getPluginId(), conf.getLabel(), parameterName));
        }

        LOGGER.debug("Primitive parameter value: {}", param);

        try {
            if (paramAnnotation.sensitive()) {
                // Only available for string parameter
                // FIXME : vérifier le fonctionnement en profondeur
                if (StringPluginParam.class.isInstance(param)) {
                    StringPluginParam spp = (StringPluginParam) param;
                    field.set(plugin, spp.getDecryptedValue());
                } else {
                    // Propagate exception
                    throw new PluginUtilsRuntimeException(String
                            .format("Exception while processing param <%s> in plugin class <%s> with param <%s>. Cannot handle sensitive value!",
                                    paramAnnotation.label(), plugin.getClass(), param));
                }
            } else {
                field.set(plugin, param.getValue());
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(String.format(EXCEPTION_WHILE_PROCESSING_PARAM_IN_PLUGIN,
                                                                paramAnnotation.label(), plugin.getClass(), param),
                    e);
        }
        LOGGER.debug(PLUGIN_PARAMETER_INJECTED, paramAnnotation.label());
    }

    /**
     * Use configured values to set field values for a parameter of type {@link PluginParameter}
     * @param <T> a {@link Plugin} type
     * @param plugin the {@link Plugin} instance
     * @param conf the plugin configuration to used
     * @param field the parameter
     * @param paramAnnotation the {@link PluginParameter} annotation
     * @param instantiatedPlugins a Map of all already instantiated plugins
     */
    private static <T> void postProcessInterface(T plugin, PluginConfiguration conf, Field field,
            PluginParameter paramAnnotation, Map<String, Object> instantiatedPlugins) {

        LOGGER.debug("Injecting nested plugin parameter \"{}\"", paramAnnotation.label());

        // Retrieve parameter name
        String parameterName = getFieldName(field, paramAnnotation);

        // Get plugin parameter configuration
        IPluginParam param = findParameter(parameterName, conf);

        if (param == null) {
            if (paramAnnotation.optional()) {
                LOGGER.debug(SKIPPING_VALUE_INJECTION_FOR_OPTIONAL, parameterName);
                return;
            } else {
                throw new IllegalArgumentException(String.format(PARAMETER_NOT_FOUND_IN_PLUGIN_CONFIGURATION,
                                                                 conf.getPluginId(), conf.getLabel(), parameterName));
            }
        }

        // Object type cannot have default value
        // Nothing to do

        // Stop if no value and optional parameter
        if (!param.hasValue() && paramAnnotation.optional()) {
            LOGGER.debug(NO_VALUE_SPECIFIED_FOR_OPTIONNAL_PARAMETER, parameterName);
            return;
        }

        // At this point, if the parameter value is not set, there is a problem
        if (!param.hasValue()) {
            throw new IllegalArgumentException(String.format(NO_PLUGIN_PARAMETER_VALUE_AND_IS_REQUIRED,
                                                             conf.getPluginId(), conf.getLabel(), parameterName));
        }

        if (!NestedPluginParam.class.isInstance(param)) {
            // Propagate exception
            throw new PluginUtilsRuntimeException(String
                    .format("Exception while processing param <%s> in plugin class <%s> with param <%s>. Wrong plugin parameter type!",
                            paramAnnotation.label(), plugin.getClass(), param));
        } else {

            NestedPluginParam npp = (NestedPluginParam) param;
            try {
                Object nestedPlugin = instantiatedPlugins.get(npp.getValue());
                field.set(plugin, nestedPlugin);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // Propagate exception
                throw new PluginUtilsRuntimeException(String.format(EXCEPTION_WHILE_PROCESSING_PARAM_IN_PLUGIN,
                                                                    paramAnnotation.label(), plugin.getClass(), param),
                        e);
            }
            LOGGER.debug(PLUGIN_PARAMETER_INJECTED, paramAnnotation.label());
        }
    }

    /**
     * Helper method  for building <b>empty</b> parameter for <b>primitive</b> type only
     */
    private static IPluginParam buildByType(Class<?> clazz, String name) {
        if (clazz.isAssignableFrom(String.class)) {
            return new StringPluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Integer.class)) {
            return new IntegerPluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Boolean.class)) {
            return new BooleanPluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Long.class)) {
            return new LongPluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Short.class)) {
            return new ShortPluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Float.class)) {
            return new FloatPluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Double.class)) {
            return new DoublePluginParam().with(name);
        }
        if (clazz.isAssignableFrom(Byte.class)) {
            return new BytePluginParam().with(name);
        }
        throw new IllegalArgumentException(String.format("Unsupported type %s", clazz));
    }

    /**
     * PrimitiveObject for the plugin parameters
     */
    public enum PrimitiveObject {

        /**
         * A primitive of {@link String}. No primitive for string! Fallback to void!
         */
        STRING(String.class, void.class, PluginParamType.STRING),

        /**
         * A primitive of {@link Byte}
         */
        BYTE(Byte.class, byte.class, PluginParamType.BYTE),

        /**
         * A primitive of {@link Short}
         */
        SHORT(Short.class, short.class, PluginParamType.SHORT),

        /**
         * A primitive of {@link Integer}
         */
        INT(Integer.class, int.class, PluginParamType.INTEGER),

        /**
         * A primitive of {@link Long}
         */
        LONG(Long.class, long.class, PluginParamType.LONG),

        /**
         * A primitive of {@link Float}
         */
        FLOAT(Float.class, float.class, PluginParamType.FLOAT),

        /**
         * A primitive of {@link Double}
         */
        DOUBLE(Double.class, double.class, PluginParamType.DOUBLE),

        /**
         * A primitive of {@link Boolean}
         */
        BOOLEAN(Boolean.class, boolean.class, PluginParamType.BOOLEAN);

        private final Class<?> type;

        private final Class<?> primitive;

        private final PluginParamType paramType;

        /**
         * Constructor
         * @param type primitive object type
         * @param primitive primitive class
         * @param paramType parameter type
         */
        PrimitiveObject(final Class<?> type, Class<?> primitive, PluginParamType paramType) {
            this.type = type;
            this.primitive = primitive;
            this.paramType = paramType;
        }

        public Class<?> getType() {
            return type;
        }

        public Class<?> getPrimitive() {
            return primitive;
        }

        public PluginParamType getParamType() {
            return paramType;
        }
    }
}
