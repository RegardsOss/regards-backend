/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginDestroy;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
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
     * Plugin interface cache, should be only populate one on startup calling {@link #setup(List)}
     */
    private static Set<String> pluginInterfaceCache;

    /**
     * Plugin class cache, should be only populate one on startup calling {@link #setup(List)}
     */
    private static Set<Class<?>> pluginCache;

    /**
     * Plugin metadata map cache, should be only populate once on startup calling {@link #setup(List)}<br/>
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
        if (reflectionPackages == null || reflectionPackages.isEmpty()) {
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
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link IPluginParam} if any.
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
     * @param conf the {@link PluginConfiguration}
     * @param pluginMetadata the {@link PluginMetaData}
     * @param instantiatedPlugins already instaniated plugins
     * @param dynamicParams an optional list of {@link IPluginParam}
     * @return an instance of a {@link Plugin} @ if a problem occurs
     */
    public static <T> T getPlugin(PluginConfiguration conf, PluginMetaData pluginMetadata,
            Map<String, Object> instantiatedPlugins, IPluginParam... dynamicParams)
            throws NotAvailablePluginConfigurationException {
        if (!conf.isActive()) {
            throw new NotAvailablePluginConfigurationException(
                    String.format("Plugin configuration <%d - %s> is not active.", conf.getId(), conf.getLabel()));
        }
        return getPlugin(conf, pluginMetadata.getPluginClassName(), instantiatedPlugins, dynamicParams);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and the plugin class name
     * @param <T> a {@link Plugin}
     * @param conf the {@link PluginConfiguration}
     * @param pluginClass the {@link Plugin} class name
     * @param dynamicParams an optional list of {@link IPluginParam}
     * @return an instance of {@link Plugin} @ if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(PluginConfiguration conf, String pluginClass, Map<String, Object> instantiatedPlugins,
            IPluginParam... dynamicParams) {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pluginClass).newInstance();
            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, conf, instantiatedPlugins, dynamicParams);
            // Autowired Spring bean in Spring IOC context
            if (PluginUtilsBean.getInstance() != null) {
                PluginUtilsBean.getInstance().processAutowiredBean(returnPlugin);
            }

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | IllegalArgumentException
                | SecurityException | ClassNotFoundException e) {
            throw new PluginUtilsRuntimeException(String.format("Cannot instantiate <%s>", pluginClass), e);
        }

        return returnPlugin;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     * @param <T> a {@link Plugin}
     * @param params a {@link List} of {@link IPluginParam}
     * @param pluginClass the required returned type
     * @param dynamicPlugins an optional {@link List} of {@link IPluginParam}
     * @return a {@link Plugin} instance
     */
    public static <T> T getPlugin(Set<IPluginParam> params, Class<T> pluginClass,
            Map<String, Object> instantiatedPlugins, IPluginParam... dynamicPlugins)
            throws NotAvailablePluginConfigurationException {
        // Build plugin metadata
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pluginClass);

        PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", params);
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata, instantiatedPlugins, dynamicPlugins);
    }

    /**
     * Look for {@link PluginDestroy} annotation and launch corresponding method if found.
     * @param <T> a {@link Plugin}
     * @param plugin the {@link Plugin} instance
     */
    public static <T> void doDestroyPlugin(final T plugin) {
        for (final Method method : ReflectionUtils.getAllDeclaredMethods(plugin.getClass())) {
            if (method.isAnnotationPresent(PluginDestroy.class)) {
                // Invoke method
                ReflectionUtils.makeAccessible(method);
                try {
                    method.invoke(plugin);
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error(String.format("Exception while invoking destroy method on plugin class <%s>.",
                                               plugin.getClass()),
                                 e);
                    throw new PluginUtilsRuntimeException(e);
                }
            }
        }
    }

    /**
     * Look for {@link PluginInit} annotation and launch corresponding method if found.
     * @param <T> a {@link Plugin}
     * @param plugin the {@link Plugin} instance @ if a problem occurs
     */
    private static <T> void doInitPlugin(final T plugin) {
        for (final Method method : ReflectionUtils.getAllDeclaredMethods(plugin.getClass())) {
            if (method.isAnnotationPresent(PluginInit.class)) {
                // Invoke method
                ReflectionUtils.makeAccessible(method);

                try {
                    method.invoke(plugin);
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error(String.format("Exception while invoking init method on plugin class <%s>.",
                                               plugin.getClass()),
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
     * @param params the plugin parameters
     * @param returnInterfaceType the required returned type
     * @return an instance @ if a problem occurs
     */
    public static <T> PluginConfiguration getPluginConfiguration(Set<IPluginParam> params,
            Class<T> returnInterfaceType) {
        // Build plugin metadata
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(returnInterfaceType);
        return new PluginConfiguration(pluginMetadata, UUID.randomUUID().toString(), params);
    }

    public static List<String> validateOnCreate(PluginConfiguration conf) {
        List<String> validationErrors = validate(conf);
        if (conf.getBusinessId() != null) {
            validationErrors.add("The plugin configuration business id must be null.");
        }
        return validationErrors;
    }

    public static List<String> validateOnUpdate(PluginConfiguration conf) {
        List<String> validationErrors = validate(conf);
        if (conf.getBusinessId() == null) {
            validationErrors.add("The plugin configuration business id required.");
        }
        return validationErrors;
    }

    /**
     * Validate the plugin configuration
     * @param conf the plugin configuration to be validated
     * @return null if there is no validation issues, the exception containing all validation errors as messages
     */
    private static List<String> validate(PluginConfiguration conf) {
        List<String> validationErrors = new ArrayList<>();
        // First lets apply equivalent to hibernate validation
        if (conf == null) {
            validationErrors.add("The plugin configuration cannot be null.");
        }
        if (conf.getPriorityOrder() == null) {
            validationErrors.add(String.format("The plugin configuration priority order is required (pluginId: %s).",
                                               conf.getPluginId()));
        }
        if (Strings.isNullOrEmpty(conf.getLabel())) {
            validationErrors.add(String.format("The plugin configuration label is required (pluginId: %s).",
                                               conf.getPluginId()));
        }
        // Now lets apply some more complicated validation that required introspection
        PluginMetaData pluginMetadata = getPlugins().get(conf.getPluginId());
        // Now that we have the metadata, lets check everything and eventually set some properties
        // as version (a null version means a plugin configuration creation
        if (conf.getVersion() == null) {
            conf.setVersion(pluginMetadata.getVersion());
        } else {
            // Check that version is the same between plugin one and plugin configuration one
            if (!Objects.equals(pluginMetadata.getVersion(), conf.getVersion())) {
                validationErrors
                        .add(String.format("Plugin configuration version (%s) is different from plugin one (%s).",
                                           conf.getVersion(), pluginMetadata.getVersion()));
            }
        }
        if (conf.getPluginId() == null) {
            conf.setPluginId(pluginMetadata.getPluginId());
        } else {
            // Check that pluginId is the same between plugin one and plugin configuration one
            if (!Objects.equals(pluginMetadata.getPluginId(), conf.getPluginId())) {
                validationErrors
                        .add(String.format("Plugin configuration pluginId (%s) is different from plugin one (%s).",
                                           conf.getPluginId(), pluginMetadata.getPluginId()));
            }
        }

        // First lets check the plugin parameters
        // first simple test, are there enough parameters?
        List<PluginParamDescriptor> pluginParametersFromMeta = pluginMetadata.getParameters();
        // the plugin configuration should not have any reference to plugin parameters that are only dynamic
        // lets check that all remaining parameters are correctly given
        for (PluginParamDescriptor plgParamMeta : pluginParametersFromMeta) {
            IPluginParam parameterFromConf = conf.getParameter(plgParamMeta.getName());
            if (!plgParamMeta.isOptional() && !plgParamMeta.getUnconfigurable() && parameterFromConf == null
                    && plgParamMeta.getDefaultValue() == null) {
                validationErrors.add(String.format("Plugin Parameter %s is missing.", plgParamMeta.getName()));
            }
            // lets add some basic type validation while we are iterating over parameters
            // in case it is not optional or missing
            if (parameterFromConf != null) {
                // FIXME check if not useful anymore
                // checkPrimitiveBoundaries(validationErrors, plgParamMeta, parameterFromConf);
            }
        }
        return validationErrors;
    }

    // FIXME check if not useful anymore
    //    private static void checkPrimitiveBoundaries(List<String> validationErrors, PluginParamDescriptor plgParamMeta,
    //            IPluginParam param) {
    //        if (plgParamMeta.getParamType() == PluginParameterType.ParamType.PRIMITIVE) {
    //            String plgParamType = plgParamMeta.getType();
    //            // first handle real primitive types then use class.forName
    //            Class<?> clazz = null;
    //            try {
    //                switch (plgParamType) {
    //                    case "long":
    //                        clazz = Long.TYPE;
    //                        break;
    //                    case "int":
    //                        clazz = Integer.TYPE;
    //                        break;
    //                    case "short":
    //                        clazz = Short.TYPE;
    //                        break;
    //                    case "byte":
    //                        clazz = Byte.TYPE;
    //                        break;
    //                    case "float":
    //                        clazz = Float.TYPE;
    //                        break;
    //                    case "double":
    //                        clazz = Double.TYPE;
    //                        break;
    //                    case "char":
    //                        clazz = Character.TYPE;
    //                        break;
    //                    case "void":
    //                        clazz = Void.TYPE;
    //                        break;
    //                    case "boolean":
    //                        clazz = Boolean.TYPE;
    //                        break;
    //                    default:
    //                        clazz = Class.forName(plgParamType);
    //                        break;
    //                }
    //                // String are not checked as we cannot imagine a string which is not valid in term of java representation
    //                // Boolean aren't either because unless its string representation is "true" if it considered false
    //                // paramStrippedValue nullability is not a problem here.
    //                // If it is null it means that the parameter is of type string with an empty value and we do not check them here.
    //                String paramStrippedValue = param.getStripParameterValue();
    //
    //                if ((plgParamMeta.isOptional() || !Strings.isNullOrEmpty(plgParamMeta.getDefaultValue()))
    //                        && (paramStrippedValue == null || paramStrippedValue.isEmpty())) {
    //                    // Skip check for optional value and default param value
    //                    return;
    //                }
    //
    //                // check numbers boundaries
    //                if (clazz.isAssignableFrom(Long.class)) {
    //                    Long.parseLong(paramStrippedValue);
    //                } else if (clazz.isAssignableFrom(Integer.class)) {
    //                    Integer.parseInt(paramStrippedValue);
    //                } else if (clazz.isAssignableFrom(Short.class)) {
    //                    Short.parseShort(paramStrippedValue);
    //                } else if (clazz.isAssignableFrom(Byte.class)) {
    //                    Byte.parseByte(paramStrippedValue);
    //                } else if (clazz.isAssignableFrom(Float.class)) {
    //                    Float.parseFloat(paramStrippedValue);
    //                } else if (clazz.isAssignableFrom(Double.class)) {
    //                    Double.parseDouble(paramStrippedValue);
    //                }
    //            } catch (ClassNotFoundException e) {
    //                LOGGER.error(e.getMessage(), e);
    //                validationErrors.add(String
    //                        .format("Plugin parameter %s is of type %s. We could not find the class descriptor associated to.",
    //                                plgParamMeta.getName(), plgParamType));
    //            } catch (NumberFormatException e) {
    //                LOGGER.error(e.getMessage(), e);
    //                validationErrors.add(String.format("Plugin Parameter %s has an invalid value. "
    //                        + "It is of type %s and could not be parsed. " + "Value might be too high or too low.",
    //                                                   plgParamMeta.getName(), clazz.getSimpleName()));
    //            }
    //        }
}
