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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginDestroy;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.bean.IPluginUtilsBean;
import fr.cnes.regards.framework.utils.plugins.bean.PluginUtilsBean;

/**
 * This class contains all the utilities to create a {@link Plugin} instance, to retrieve all annotated plugins and to
 * create a {@link PluginConfiguration}.
 *
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
     *
     * @param prefix a package prefix used for the scan
     * @param prefixes a {@link List} of package to scan to find the {@link Plugin} and {@link PluginInterface}
     * @return all class annotated {@link Plugin}
     */
    public static Map<String, PluginMetaData> getPlugins(final String prefix, final List<String> prefixes) {
        final Map<String, PluginMetaData> plugins = new HashMap<>();

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(prefix);
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
     *
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return all class annotated {@link Plugin}
     */
    public static ConcurrentMap<String, PluginMetaData> getPlugins(final List<String> prefixes) {
        final ConcurrentMap<String, PluginMetaData> plugins = new ConcurrentHashMap<>();

        for (final String p : prefixes) {
            plugins.putAll(getPlugins(p, prefixes));
        }

        return plugins;
    }

    /**
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link PluginParameter} if any.
     *
     * @param pluginClass a class that must contains a {@link Plugin} annotation
     * @param prefixes packages to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(final Class<?> pluginClass, final String... prefixes) {
        return createPluginMetaData(pluginClass, Lists.newArrayList(prefixes));
    }

    /**
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link PluginParameter} if any.
     *
     * @param pluginClass a class that must contains a {@link Plugin} annotation
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(final Class<?> pluginClass, final List<String> prefixes) {
        // Get implementation associated annotations
        final Plugin plugin = pluginClass.getAnnotation(Plugin.class);

        // Init plugin metadata
        final PluginMetaData pluginMetaData = new PluginMetaData(plugin);
        pluginMetaData.setPluginClassName(pluginClass.getCanonicalName());

        // Search the plugin type of the plugin class : ie the interface has the @PluginInterface annotation
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
     *
     * @param <T> a {@link Plugin}
     * @param pluginConf the {@link PluginConfiguration}
     * @param pluginMetadata the {@link PluginMetaData}
     * @param prefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param instantiatedPluginMap already instaniated plugins
     * @param pluginParameters an optional list of {@link PluginParameter}
     * @return an instance of a {@link Plugin} @ if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(PluginConfiguration pluginConf, PluginMetaData pluginMetadata, List<String> prefixs,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... pluginParameters) {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pluginMetadata.getPluginClassName()).newInstance();

            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, pluginConf, prefixs, instantiatedPluginMap,
                                             pluginParameters);

            if (PluginUtilsBean.getInstance() != null) {
                PluginUtilsBean.getInstance().processAutowiredBean(returnPlugin);
            }

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | ClassNotFoundException e) {
            throw new PluginUtilsRuntimeException(
                    String.format(CANNOT_INSTANTIATE, pluginMetadata.getPluginClassName()), e);

        }

        return returnPlugin;
    }

    public static <T> T getPlugin(PluginConfiguration pluginConf, PluginMetaData pluginMetadata,
            IPluginUtilsBean pluginUtilsBean, List<String> prefixes, Map<Long, Object> instantiatedPluginMap,
            PluginParameter... pluginParameters) {
        return PluginUtils.getPlugin(pluginConf, pluginMetadata, prefixes, instantiatedPluginMap, pluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and the plugin class name
     *
     * @param <T> a {@link Plugin}
     * @param pluginConf the {@link PluginConfiguration}
     * @param pluginClassName the {@link Plugin} class name
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pluginParameters an optional list of {@link PluginParameter}
     * @return an instance of {@link Plugin} @ if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(PluginConfiguration pluginConf, String pluginClassName, List<String> prefixes,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... pluginParameters) {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pluginClassName).newInstance();

            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, pluginConf, prefixes, instantiatedPluginMap,
                                             pluginParameters);

            if (PluginUtilsBean.getInstance() != null) {
                PluginUtilsBean.getInstance().processAutowiredBean(returnPlugin);
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
     *
     * @param <T> a {@link Plugin}
     * @param parameters a {@link List} of {@link PluginParameter}
     * @param returnInterfaceType the required returned type
     * @param pluginUtilsBean a {@link PluginUtilsBean} containing your own
     *            {@link org.springframework.beans.factory.BeanFactory}
     * @param prefixes a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance @ if a problem occurs
     */
    public static <T> T getPlugin(List<PluginParameter> parameters, Class<T> returnInterfaceType,
            IPluginUtilsBean pluginUtilsBean, List<String> prefixes, Map<Long, Object> instantiatedPluginMap,
            PluginParameter... pluginParameters) {
        return PluginUtils.getPlugin(parameters, returnInterfaceType, prefixes, instantiatedPluginMap,
                                     pluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     *
     * @param <T> a {@link Plugin}
     * @param parameters a {@link List} of {@link PluginParameter}
     * @param returnInterfaceType the required returned type
     * @param prefixes a {@link List} of package to scan to find the {@link Plugin} and {@link PluginInterface}
     * @param pluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance
     */
    public static <T> T getPlugin(List<PluginParameter> parameters, Class<T> returnInterfaceType, List<String> prefixes,
            Map<Long, Object> instantiatedPluginMap, PluginParameter... pluginParameters) {
        // Build plugin metadata
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(returnInterfaceType, prefixes);

        PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", parameters);
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata, prefixes, instantiatedPluginMap,
                                     pluginParameters);
    }

    /**
     * Look for {@link PluginDestroy} annotation and launch corresponding method if found.
     * @param <T> a {@link Plugin}
     * @param pluginInstance the {@link Plugin} instance
     */
    public static <T> void doDestroyPlugin(final T pluginInstance) {
        final Method[] allMethods = pluginInstance.getClass().getDeclaredMethods();
        for (final Method method : allMethods) {
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
        final Method[] allMethods = pluginInstance.getClass().getDeclaredMethods();
        for (final Method method : allMethods) {
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
     *
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
}
