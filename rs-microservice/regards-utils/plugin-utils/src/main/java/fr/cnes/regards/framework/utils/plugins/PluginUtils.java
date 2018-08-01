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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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

/**
 * This class contains all the utilities to create a {@link Plugin} instance, to retrieve all annotated plugins and to
 * create a {@link PluginConfiguration}.
 * @author Christophe Mertz
 */
public final class PluginUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);

    /**
     * Message error plugin instantiate
     */
    private static final String CANNOT_INSTANTIATE = "Cannot instantiate <%s>";

    /**
     * Constructor
     */
    private PluginUtils() {
        // Static class
    }

    /**
     * Retrieve all annotated plugins (@see {@link Plugin}) and initialize a map whose key is the {@link Plugin}
     * identifier and value the required plugin metadata.
     * @param prefix a package prefix used for the scan
     * @param prefixes a {@link List} of package to scan to find the {@link Plugin} and {@link PluginInterface}
     * @return all class annotated {@link Plugin}
     */
    public static Map<String, PluginMetaData> getPlugins(String prefix, List<String> prefixes) {
        final Map<String, PluginMetaData> plugins = new HashMap<>();

        // Scan class path with Reflections library
        final Reflections reflections = ReflectionUtils.getReflections();
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(Plugin.class, true);

        // Create a plugin object for each class
        for (final Class<?> pluginClass : annotatedPlugins) {

            // Create plugin metadata
            final PluginMetaData plugin = PluginUtils.createPluginMetaData(pluginClass, prefixes);

            // Check a plugin does not already exists with the same plugin id
            if (plugins.containsKey(plugin.getPluginId())) {
                final PluginMetaData pMeta = plugins.get(plugin.getPluginId());
                final String message = String
                        .format("Plugin identifier must be unique : %s for plugin \"%s\" already used in plugin \"%s\"!",
                                plugin.getPluginId(), plugin.getPluginClassName(), pMeta.getPluginClassName());
                LOGGER.warn(message);
            }

            // Store plugin reference
            plugins.put(plugin.getPluginId(), plugin);

            LOGGER.info(String.format("Plugin \"%s\" with identifier \"%s\" loaded.", plugin.getPluginClassName(),
                                      plugin.getPluginId()));
        }
        return plugins;
    }

    /**
     * Retrieve all annotated plugins (@see {@link Plugin}) and initialize a map whose key is the {@link Plugin}
     * identifier and value the required {@link PluginMetaData}.
     * <b>Note: </b> This method is used by PluginService which is used in multi-thread environment (see IngesterService
     * and CrawlerService) so ConcurrentHashMap is used instead of HashMap
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return all class annotated {@link Plugin}
     */
    public static Map<String, PluginMetaData> getPlugins(List<String> prefixes) {
        ConcurrentMap<String, PluginMetaData> pluginMap = new ConcurrentHashMap<>();

        for (String p : prefixes) {
            pluginMap.putAll(getPlugins(p, prefixes));
        }

        return pluginMap;
    }

    /**
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link PluginParameter} if any.
     * @param pluginClass a class that must contains a {@link Plugin} annotation
     * @param prefixes packages to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(final Class<?> pluginClass, final String... prefixes) {
        return createPluginMetaData(pluginClass, Lists.newArrayList(prefixes));
    }

    /**
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link PluginParameter} if any.
     * @param pluginClass a class that must contains a {@link Plugin} annotation
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(final Class<?> pluginClass, final List<String> prefixes) {
        // Get implementation associated annotations
        Plugin plugin = pluginClass.getAnnotation(Plugin.class);

        // Init plugin metadata
        PluginMetaData pluginMetaData = new PluginMetaData(plugin);

        // Manage markdown description
        String markdown = AnnotationUtils.loadMarkdown(pluginClass, plugin.markdown());
        pluginMetaData.setMarkdown(markdown);

        pluginMetaData.setPluginClassName(pluginClass.getCanonicalName());

        // Search the plugin type of the plugin class : i.e. the interface that has the @PluginInterface annotation
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(prefixes);
        List<String> types = new ArrayList<>(); // FIXME: is really used?

        for (Class<?> aInterface : TypeToken.of(pluginClass).getTypes().interfaces().rawTypes()) {
            types.add(aInterface.getCanonicalName());
            if (pluginInterfaces.contains(aInterface.getCanonicalName())) {
                pluginMetaData.getInterfaceNames().add(aInterface.getCanonicalName());
            }
        }

        // Try to detect parameters if any
        pluginMetaData.setParameters(PluginParameterUtils.getParameters(pluginClass, prefixes));
        return pluginMetaData;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     * @param <T> a {@link Plugin}
     * @param pluginConf the {@link PluginConfiguration}
     * @param pluginMetadata the {@link PluginMetaData}
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param instantiatedPluginMap already instaniated plugins
     * @param dynamicPluginParameters an optional list of {@link PluginParameter}
     * @return an instance of a {@link Plugin} @ if a problem occurs
     */
    public static <T> T getPlugin(PluginConfiguration pluginConf, PluginMetaData pluginMetadata, List<String> prefixes,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... dynamicPluginParameters) {
        return getPlugin(pluginConf, pluginMetadata.getPluginClassName(), prefixes, instantiatedPluginMap,
                         dynamicPluginParameters);
    }

    public static <T> T getPlugin(PluginConfiguration pluginConf, PluginMetaData pluginMetadata,
            IPluginUtilsBean pluginUtilsBean, List<String> prefixes, Map<Long, Object> instantiatedPluginMap,
            PluginParameter... dynamicPluginParameters) {
        return PluginUtils
                .getPlugin(pluginConf, pluginMetadata, prefixes, instantiatedPluginMap, dynamicPluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and the plugin class name
     * @param <T> a {@link Plugin}
     * @param pluginConf the {@link PluginConfiguration}
     * @param pluginClassName the {@link Plugin} class name
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param dynamicPluginParameters an optional list of {@link PluginParameter}
     * @return an instance of {@link Plugin} @ if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(PluginConfiguration pluginConf, String pluginClassName, List<String> prefixes,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... dynamicPluginParameters) {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pluginClassName).newInstance();

            if (PluginUtilsBean.getInstance() != null) {
                // Post process parameters in Spring context
                PluginParameterUtils
                        .postProcess(PluginUtilsBean.getInstance().getGson(), returnPlugin, pluginConf, prefixes,
                                     instantiatedPluginMap, dynamicPluginParameters);
                PluginUtilsBean.getInstance().processAutowiredBean(returnPlugin);
            } else {
                // Post process parameters without Spring
                PluginParameterUtils
                        .postProcess(Optional.empty(), returnPlugin, pluginConf, prefixes, instantiatedPluginMap,
                                     dynamicPluginParameters);
            }

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | IllegalArgumentException | SecurityException | ClassNotFoundException e) {
            throw new PluginUtilsRuntimeException(String.format(CANNOT_INSTANTIATE, pluginClassName), e);
        }

        return returnPlugin;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     * @param <T> a {@link Plugin}
     * @param parameters a {@link List} of {@link PluginParameter}
     * @param pluginClass the required returned type
     * @param pluginUtilsBean a {@link PluginUtilsBean} containing your own
     * {@link org.springframework.beans.factory.BeanFactory}
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance @ if a problem occurs
     */
    public static <T> T getPlugin(List<PluginParameter> parameters, Class<T> pluginClass,
            IPluginUtilsBean pluginUtilsBean, List<String> prefixes, Map<Long, Object> instantiatedPluginMap,
            PluginParameter... pluginParameters) {
        return PluginUtils.getPlugin(parameters, pluginClass, prefixes, instantiatedPluginMap, pluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     * @param <T> a {@link Plugin}
     * @param parameters a {@link List} of {@link PluginParameter}
     * @param pluginClass the required returned type
     * @param prefixes a {@link List} of package to scan to find the {@link Plugin} and {@link PluginInterface}
     * @param dynamicPluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance
     */
    public static <T> T getPlugin(List<PluginParameter> parameters, Class<T> pluginClass, List<String> prefixes,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... dynamicPluginParameters) {
        // Build plugin metadata
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pluginClass, prefixes);

        PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", parameters);
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata, prefixes, instantiatedPluginMap,
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
                                               pluginInstance.getClass()), e);
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
                                               pluginInstance.getClass()), e);
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
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return an instance @ if a problem occurs
     */
    public static <T> PluginConfiguration getPluginConfiguration(final List<PluginParameter> parameters,
            final Class<T> returnInterfaceType, final List<String> prefixes) {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(returnInterfaceType, prefixes);

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
            validationErrors
                    .add(String.format("The plugin configuration priority order is required (pluginId: %s).",
                                       pluginConfiguration.getPluginId()));
        }
        if (Strings.isNullOrEmpty(pluginConfiguration.getLabel())) {
            validationErrors.add(String.format("The plugin configuration label is required (pluginId: %s).",
                                               pluginConfiguration.getPluginId()));
        }
        // Now lets apply some more complicated validation that required introspection
        try {
            List<String> packages = new ArrayList<>();
            Class<?> pluginClass = Class.forName(pluginConfiguration.getPluginClassName());
            // Let's get the plugin interfaces packages
            for (String interfaceName : pluginConfiguration.getInterfaceNames()) {
                packages.add(Class.forName(interfaceName).getPackage().getName());
            }
            // Don't forget to add the implementation package
            packages.add(pluginClass.getPackage().getName());
            PluginMetaData pluginMetadata = createPluginMetaData(pluginClass, packages);
            // Now that we have the metadata, lets check everything and eventualy set some properties
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
            //    first simple test, are there enough parameters?
            List<PluginParameterType> pluginParametersFromMeta = pluginMetadata.getParameters();
            //    the plugin configuration should not have any reference to plugin parameters that are only dynamic
            //    lets check that all remaining parameters are correctly given
            for (PluginParameterType plgParamMeta : pluginParametersFromMeta) {
                if (!plgParamMeta.isOptional() && !plgParamMeta.getUnconfigurable() && (
                        pluginConfiguration.getParameter(plgParamMeta.getName()) == null
                                && plgParamMeta.getDefaultValue() == null)) {
                    validationErrors.add(String.format("Plugin Parameter %s is missing.", plgParamMeta.getName()));
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            validationErrors.add(e.getMessage());
        }
        return validationErrors.isEmpty() ? null : new EntityInvalidException(validationErrors);
    }
}
