/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginDestroy;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;
import fr.cnes.regards.framework.utils.plugins.bean.IPluginUtilsBean;
import fr.cnes.regards.framework.utils.plugins.bean.PluginUtilsBean;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * This class contains all the utilities to create a {@link Plugin} instance, to retrieve all annotated plugins and to
 * create a {@link PluginConfiguration}.<br/>
 * Before using it, you have to call a setup method.
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public final class PluginUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);

    private static final String HR = "####################################################";

    /**
     * Message error plugin instantiate
     */
    private static final String CANNOT_INSTANTIATE = "Cannot instantiate <%s>";

    /**
     * Plugin interface cache, should be only populate one on startup calling {@link #setup(List)}
     */
    private static Set<String> pluginInterfaceCache;

    /**
     * Plugin class cache, should be only populate one on startup calling {@link #setup(List)}
     */
    private static Set<Class<?>> pluginCache;

    /**
     * Plugin metadata map cache, should be only populate one on startup calling {@link #setup(List)}<br/>
     * Map key is the plugin identifier, map value is the related plugin metadata.
     */
    private static Map<String, PluginMetaData> pluginMetadataCache = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    private PluginUtils() {
        // Static class
    }

    /**
     * Method to set up plugin context with default package <code>fr.cnes.regards</code><br/>
     * <b>Must be call on startup in a thread safe manner</b>
     */
    public static void setup() {
        setup(new ArrayList<>());
    }

    /**
     * Method to set up plugin context.<br/>
     * <b>Must be call on startup in a thread safe manner</b>
     * @param reflectionPackage package to scan
     */
    public static void setup(String reflectionPackage) {
        setup(Collections.singletonList(reflectionPackage));
    }

    /**
     * Method to set up plugin context.<br/>
     * <b>Must be call on startup in a thread safe manner</b>
     * @param reflectionPackages packages to scan
     * <b>Note: this method is synchronized due to pluginInterfaceCache, pluginCache and pluginMetadataCache
     * initializations. This not a problem because this method should be called only once.</b>
     */
    public static synchronized void setup(List<String> reflectionPackages) {
        LOGGER.info("{} Loading plugins...", HR);
        // Initialize reflection tool
        Reflections reflections;
        if ((reflectionPackages == null) || reflectionPackages.isEmpty()) {
            String defaultPackage = "fr.cnes.regards";
            LOGGER.info("System will look for plugins in default package: {}", defaultPackage);
            reflections = new Reflections(defaultPackage);
        } else {
            StringJoiner customPackages = new StringJoiner(",");
            reflectionPackages.forEach(customPackages::add);
            LOGGER.info("System will look for plugins in custom package(s): {}", customPackages.toString());
            Configuration configuration = ConfigurationBuilder.build(reflectionPackages.toArray(new Object[0]));
            reflections = new Reflections(configuration);
        }

        // Initialize plugin interfaces
        pluginInterfaceCache = new HashSet<>();
        Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(PluginInterface.class, true);
        annotatedPlugins.forEach(i -> pluginInterfaceCache.add(i.getCanonicalName()));

        // Initialize plugins
        pluginCache = reflections.getTypesAnnotatedWith(Plugin.class, true);

        // Initialize plugin metadata map
        pluginMetadataCache = new ConcurrentHashMap<>();

        // Create a plugin object for each class
        for (Class<?> pluginClass : pluginCache) {

            // Create plugin metadata
            PluginMetaData plugin = PluginUtils.createPluginMetaData(pluginClass);

            // Check a plugin does not already exists with the same plugin id
            if (pluginMetadataCache.containsKey(plugin.getPluginId())) {
                PluginMetaData pMeta = pluginMetadataCache.get(plugin.getPluginId());
                String message = String
                        .format("Plugin identifier must be unique : %s for plugin \"%s\" already used in plugin \"%s\"!",
                                plugin.getPluginId(), plugin.getPluginClassName(), pMeta.getPluginClassName());
                LOGGER.warn(message);
            }

            // Store plugin reference
            pluginMetadataCache.put(plugin.getPluginId(), plugin);

            LOGGER.info(String.format("Plugin \"%s\" with identifier \"%s\" loaded.", plugin.getPluginClassName(),
                                      plugin.getPluginId()));
        }
        LOGGER.info("{} Plugins loaded!", HR);
    }

    public static Set<String> getPluginInterfaces() {
        return pluginInterfaceCache;
    }

    /**
     * Retrieve all annotated plugins (@see {@link Plugin}) and initialize a map whose key is the {@link Plugin}
     * identifier and value the required plugin metadata.
     * @return all class annotated {@link Plugin}
     */
    public static Map<String, PluginMetaData> getPlugins() {
        return pluginMetadataCache;
    }

    /**
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link PluginParameter} if any.
     * @param pluginClass a class that must contains a {@link Plugin} annotation
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(Class<?> pluginClass) {
        // Get implementation associated annotations
        Plugin plugin = pluginClass.getAnnotation(Plugin.class);

        // Init plugin metadata
        PluginMetaData pluginMetaData = new PluginMetaData(plugin);

        // Manage markdown description
        String markdown = AnnotationUtils.loadMarkdown(pluginClass, plugin.markdown());
        pluginMetaData.setMarkdown(markdown);

        pluginMetaData.setPluginClassName(pluginClass.getCanonicalName());

        // Search the plugin type of the plugin class : i.e. the interface that has the @PluginInterface annotation
        for (Class<?> aInterface : TypeToken.of(pluginClass).getTypes().interfaces().rawTypes()) {
            if (pluginInterfaceCache.contains(aInterface.getCanonicalName())) {
                pluginMetaData.getInterfaceNames().add(aInterface.getCanonicalName());
            }
        }

        // Try to detect parameters if any
        pluginMetaData.setParameters(PluginParameterUtils.getParameters(pluginClass));
        return pluginMetaData;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     * @param <T> a {@link Plugin}
     * @param pluginConf the {@link PluginConfiguration}
     * @param pluginMetadata the {@link PluginMetaData}
     * @param instantiatedPluginMap already instaniated plugins
     * @param dynamicPluginParameters an optional list of {@link PluginParameter}
     * @return an instance of a {@link Plugin} @ if a problem occurs
     * @throws NotAvailablePluginConfigurationException
     */
    public static <T> T getPlugin(PluginConfiguration pluginConf, PluginMetaData pluginMetadata,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... dynamicPluginParameters)
            throws NotAvailablePluginConfigurationException {
        if (!pluginConf.isActive()) {
            throw new NotAvailablePluginConfigurationException(
                    String.format("Plugin configuration <%d - %s> is not active.", pluginConf.getId(),
                                  pluginConf.getLabel()));
        }
        return getPlugin(pluginConf, pluginMetadata.getPluginClassName(), instantiatedPluginMap,
                         dynamicPluginParameters);
    }

    public static <T> T getPlugin(PluginConfiguration pluginConf, PluginMetaData pluginMetadata,
            IPluginUtilsBean pluginUtilsBean, Map<Long, Object> instantiatedPluginMap,
            PluginParameter... dynamicPluginParameters) throws NotAvailablePluginConfigurationException {
        return PluginUtils.getPlugin(pluginConf, pluginMetadata, instantiatedPluginMap, dynamicPluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and the plugin class name
     * @param <T> a {@link Plugin}
     * @param pluginConf the {@link PluginConfiguration}
     * @param pluginClassName the {@link Plugin} class name
     * @param dynamicPluginParameters an optional list of {@link PluginParameter}
     * @return an instance of {@link Plugin} @ if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(PluginConfiguration pluginConf, String pluginClassName,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... dynamicPluginParameters) {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pluginClassName).newInstance();

            if (PluginUtilsBean.getInstance() != null) {
                // Post process parameters in Spring context
                PluginParameterUtils.postProcess(PluginUtilsBean.getInstance().getGson(), returnPlugin, pluginConf,
                                                 instantiatedPluginMap, dynamicPluginParameters);
                PluginUtilsBean.getInstance().processAutowiredBean(returnPlugin);
            } else {
                // Post process parameters without Spring
                PluginParameterUtils.postProcess(Optional.empty(), returnPlugin, pluginConf, instantiatedPluginMap,
                                                 dynamicPluginParameters);
            }

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | IllegalArgumentException
                | SecurityException | ClassNotFoundException e) {
            throw new PluginUtilsRuntimeException(String.format(CANNOT_INSTANTIATE, pluginClassName), e);
        }

        return returnPlugin;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     * @param <T> a {@link Plugin}
     * @param parameters a {@link List} of {@link PluginParameter}
     * @param pluginClass the required returned type
     * @param dynamicPluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance
     * @throws NotAvailablePluginConfigurationException
     */
    public static <T> T getPlugin(Set<PluginParameter> parameters, Class<T> pluginClass,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... dynamicPluginParameters)
            throws NotAvailablePluginConfigurationException {
        // Build plugin metadata
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pluginClass);

        PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", parameters);
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata, instantiatedPluginMap,
                                     dynamicPluginParameters);
    }

    /**
     * Look for {@link PluginDestroy} annotation and launch corresponding method if found.
     * @param <T> a {@link Plugin}
     * @param pluginInstance the {@link Plugin} instance
     */
    public static <T> void doDestroyPlugin(final T pluginInstance) {
        for (final Method method : ReflectionUtils.getAllDeclaredMethods(pluginInstance.getClass())) {
            if (method.isAnnotationPresent(PluginDestroy.class)) {
                // Invoke method
                ReflectionUtils.makeAccessible(method);

                try {
                    method.invoke(pluginInstance);
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error(String.format("Exception while invoking destroy method on plugin class <%s>.",
                                               pluginInstance.getClass()),
                                 e);
                    throw new PluginUtilsRuntimeException(e);
                }
            }
        }
    }

    /**
     * Look for {@link PluginInit} annotation and launch corresponding method if found.
     * @param <T> a {@link Plugin}
     * @param pluginInstance the {@link Plugin} instance @ if a problem occurs
     */
    private static <T> void doInitPlugin(final T pluginInstance) {
        for (final Method method : ReflectionUtils.getAllDeclaredMethods(pluginInstance.getClass())) {
            if (method.isAnnotationPresent(PluginInit.class)) {
                // Invoke method
                ReflectionUtils.makeAccessible(method);

                try {
                    method.invoke(pluginInstance);
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error(String.format("Exception while invoking init method on plugin class <%s>.",
                                               pluginInstance.getClass()),
                                 e);
                    if (e.getCause() instanceof PluginUtilsRuntimeException) {
                        throw (PluginUtilsRuntimeException) e.getCause();
                    } else {
                        throw new PluginUtilsRuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Create an instance of {@link PluginConfiguration}
     * @param <T> a plugin
     * @param parameters the plugin parameters
     * @param returnInterfaceType the required returned type
     * @return an instance @ if a problem occurs
     */
    public static <T> PluginConfiguration getPluginConfiguration(Set<PluginParameter> parameters,
            Class<T> returnInterfaceType) {
        // Build plugin metadata
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(returnInterfaceType);
        return new PluginConfiguration(pluginMetadata, UUID.randomUUID().toString(), parameters);
    }

    /**
     * Validate the plugin configuration
     * @param pluginConfiguration the plugin configuration to be validated
     * @return null if there is no validation issues, the exception containing all validation errors as messages
     */
    public static EntityInvalidException validate(PluginConfiguration pluginConfiguration) {
        List<String> validationErrors = new ArrayList<>();
        // First lets apply equivalent to hibernate validation
        if (pluginConfiguration == null) {
            validationErrors.add("The plugin configuration cannot be null.");
            return new EntityInvalidException(validationErrors);
        }
        if (pluginConfiguration.getPriorityOrder() == null) {
            validationErrors.add(String.format("The plugin configuration priority order is required (pluginId: %s).",
                                               pluginConfiguration.getPluginId()));
        }
        if (Strings.isNullOrEmpty(pluginConfiguration.getLabel())) {
            validationErrors.add(String.format("The plugin configuration label is required (pluginId: %s).",
                                               pluginConfiguration.getPluginId()));
        }
        // Now lets apply some more complicated validation that required introspection
        try {
            Class<?> pluginClass = Class.forName(pluginConfiguration.getPluginClassName());
            PluginMetaData pluginMetadata = createPluginMetaData(pluginClass);
            // Now that we have the metadata, lets check everything and eventually set some properties
            // as version (a null version means a plugin configuration creation
            if (pluginConfiguration.getVersion() == null) {
                pluginConfiguration.setVersion(pluginMetadata.getVersion());
            } else {
                // Check that version is the same between plugin one and plugin configuration one
                if (!Objects.equals(pluginMetadata.getVersion(), pluginConfiguration.getVersion())) {
                    validationErrors
                            .add(String.format("Plugin configuration version (%s) is different from plugin one (%s).",
                                               pluginConfiguration.getVersion(), pluginMetadata.getVersion()));
                }
            }
            if (pluginConfiguration.getPluginId() == null) {
                pluginConfiguration.setPluginId(pluginMetadata.getPluginId());
            } else {
                // Check that pluginId is the same between plugin one and plugin configuration one
                if (!Objects.equals(pluginMetadata.getPluginId(), pluginConfiguration.getPluginId())) {
                    validationErrors
                            .add(String.format("Plugin configuration pluginId (%s) is different from plugin one (%s).",
                                               pluginConfiguration.getPluginId(), pluginMetadata.getPluginId()));
                }
            }

            // First lets check the plugin parameters
            // first simple test, are there enough parameters?
            List<PluginParameterType> pluginParametersFromMeta = pluginMetadata.getParameters();
            // the plugin configuration should not have any reference to plugin parameters that are only dynamic
            // lets check that all remaining parameters are correctly given
            for (PluginParameterType plgParamMeta : pluginParametersFromMeta) {
                PluginParameter parameterFromConf = pluginConfiguration.getParameter(plgParamMeta.getName());
                if (!plgParamMeta.isOptional() && !plgParamMeta.getUnconfigurable() && (parameterFromConf == null)
                        && (plgParamMeta.getDefaultValue() == null)) {
                    validationErrors.add(String.format("Plugin Parameter %s is missing.", plgParamMeta.getName()));
                }
                // lets add some basic type validation while we are iterating over parameters
                // in case it is not optional or missing
                if (parameterFromConf != null) {
                    checkPrimitiveBoundaries(validationErrors, plgParamMeta, parameterFromConf);
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            validationErrors.add(e.getMessage());
        }
        return validationErrors.isEmpty() ? null : new EntityInvalidException(validationErrors);
    }

    private static void checkPrimitiveBoundaries(List<String> validationErrors, PluginParameterType plgParamMeta,
            PluginParameter parameterFromConf) {
        if (plgParamMeta.getParamType() == PluginParameterType.ParamType.PRIMITIVE) {
            String plgParamType = plgParamMeta.getType();
            // first handle real primitive types then use class.forName
            Class<?> clazz = null;
            try {
                switch (plgParamType) {
                    case "long":
                        clazz = Long.TYPE;
                        break;
                    case "int":
                        clazz = Integer.TYPE;
                        break;
                    case "short":
                        clazz = Short.TYPE;
                        break;
                    case "byte":
                        clazz = Byte.TYPE;
                        break;
                    case "float":
                        clazz = Float.TYPE;
                        break;
                    case "double":
                        clazz = Double.TYPE;
                        break;
                    case "char":
                        clazz = Character.TYPE;
                        break;
                    case "void":
                        clazz = Void.TYPE;
                        break;
                    case "boolean":
                        clazz = Boolean.TYPE;
                        break;
                    default:
                        clazz = Class.forName(plgParamType);
                        break;
                }
                // String are not checked as we cannot imagine a string which is not valid in term of java representation
                // Boolean aren't either because unless its string representation is "true" if it considered false
                // paramStrippedValue nullability is not a problem here.
                // If it is null it means that the parameter is of type string with an empty value and we do not check them here.
                String paramStrippedValue = parameterFromConf.getStripParameterValue();

                if ((plgParamMeta.isOptional() || !Strings.isNullOrEmpty(plgParamMeta.getDefaultValue()))
                        && ((paramStrippedValue == null) || paramStrippedValue.isEmpty())) {
                    // Skip check for optional value and default param value
                    return;
                }

                // check numbers boundaries
                if (clazz.isAssignableFrom(Long.class)) {
                    Long.parseLong(paramStrippedValue);
                } else if (clazz.isAssignableFrom(Integer.class)) {
                    Integer.parseInt(paramStrippedValue);
                } else if (clazz.isAssignableFrom(Short.class)) {
                    Short.parseShort(paramStrippedValue);
                } else if (clazz.isAssignableFrom(Byte.class)) {
                    Byte.parseByte(paramStrippedValue);
                } else if (clazz.isAssignableFrom(Float.class)) {
                    Float.parseFloat(paramStrippedValue);
                } else if (clazz.isAssignableFrom(Double.class)) {
                    Double.parseDouble(paramStrippedValue);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                validationErrors.add(String
                        .format("Plugin parameter %s is of type %s. We could not find the class descriptor associated to.",
                                plgParamMeta.getName(), plgParamType));
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage(), e);
                validationErrors.add(String.format("Plugin Parameter %s has an invalid value. "
                        + "It is of type %s and could not be parsed. " + "Value might be too high or too low.",
                                                   plgParamMeta.getName(), clazz.getSimpleName()));
            }
        }
    }
}
