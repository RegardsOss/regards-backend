/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 *
 * Plugin utilities
 *
 * @author cmertz
 */
public final class PluginUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);

    /**
     *
     * Constructor
     *
     */
    private PluginUtils() {
        // Static class
    }

    /**
     *
     * Retrieve all annotated plugin (@see {@link Plugin}) and init a map whose key is the plugin identifier and value
     * the required plugin metadata.
     * 
     * @param pPrefix
     *            a package prefix used for the search
     * @return all class annotated {@link Plugin}
     * @throws PluginUtilsException
     *             a pluginId is found a twice
     */
    public static Map<String, PluginMetaData> getPlugins(final String pPrefix) throws PluginUtilsException {
        final Map<String, PluginMetaData> plugins = new HashMap<>();

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(Plugin.class);

        // Create a plugin object for each class
        for (final Class<?> pluginClass : annotatedPlugins) {

            // Create plugin metadata
            final PluginMetaData plugin = PluginUtils.createPluginMetaData(pluginClass);

            // Check a plugin does not already exists with the same plugin id
            if (plugins.containsKey(plugin.getPluginId())) {
                final PluginMetaData pMeta = plugins.get(plugin.getPluginId());
                final String message = String
                        .format("Plugin identifier must be unique : %s for plugin \"%s\" already used in plugin \"%s\"!",
                                plugin.getPluginId(), plugin.getPluginClassName(), pMeta.getPluginClassName());
                throw new PluginUtilsException(message);
            }

            // Store plugin reference
            plugins.put(plugin.getPluginId(), plugin);
            LOGGER.info(String.format("Plugin \"%s\" with identifier \"%s\" loaded.", plugin.getPluginClassName(),
                                      plugin.getPluginId()));
        }
        return plugins;
    }

    /**
     *
     * Create plugin metadata based on its annotations {@link Plugin} and {@link PluginParameters} if any.
     *
     * @param pPluginClass
     *            a class that must contains a {@link Plugin} annotation
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(final Class<?> pPluginClass) {
        final PluginMetaData pluginMetaData;

        // Get implementation associated annotations
        final Plugin plugin = pPluginClass.getAnnotation(Plugin.class);

        // Init plugin metadata
        pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(pPluginClass.getCanonicalName());

        // Manage plugin id
        if ("".equals(plugin.id())) {
            pluginMetaData.setPluginId(pPluginClass.getCanonicalName());
        } else {
            pluginMetaData.setPluginId(plugin.id());
        }
        pluginMetaData.setAuthor(plugin.author());
        pluginMetaData.setVersion(plugin.version());
        pluginMetaData.setDescription(plugin.description());

        // Try to detect parameters if any
        pluginMetaData.setParameters(PluginParameterUtils.getParameters(pPluginClass));

        return pluginMetaData;
    }

    /**
     *
     * Create an instance of plugin based on its configuration and metadata
     *
     * @param <T>
     *            a plugin
     * @param pPluginConf
     *            the {@link PluginConfiguration}
     * @param pPluginMetadata
     *            the {@link PluginMetaData}
     * @param pPluginParameters
     *            an optional list of {@link PluginParameter}
     * 
     * @return an instance of plugin
     * 
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> T getPlugin(final PluginConfiguration pPluginConf, final PluginMetaData pPluginMetadata,
            final PluginParameter... pPluginParameters) throws PluginUtilsException {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pPluginMetadata.getPluginClassName()).newInstance();

            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, pPluginConf, pPluginParameters);

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | ClassNotFoundException e) {
            throw new PluginUtilsException(
                    String.format("Cannot instantiate <%s>", pPluginMetadata.getPluginClassName()), e);

        }

        return returnPlugin;
    }

    /**
     *
     * Create an instance of plugin based on its configuration and the plugin class name
     *
     * @param <T>
     *            a plugin
     * @param pPluginConf
     *            the {@link PluginConfiguration}
     * @param pPluginClassName
     *            the plugin class name
     * @param pPluginParameters
     *            an optional list of {@link PluginParameter}
     * 
     * @return an instance of plugin
     * 
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> T getPlugin(final PluginConfiguration pPluginConf, final String pPluginClassName,
            final PluginParameter... pPluginParameters) throws PluginUtilsException {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pPluginClassName).newInstance();

            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, pPluginConf, pPluginParameters);

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | IllegalArgumentException
                | SecurityException | ClassNotFoundException e) {
            throw new PluginUtilsException(String.format("Cannot instantiate <%s>", pPluginClassName), e);
        }

        return returnPlugin;
    }

    /**
     *
     * Create an instance of plugin based on its configuration and metadata
     *
     * @param <T>
     *            a plugin
     * @param pParameters
     *            the plugin parameters
     * @param pReturnInterfaceType
     *            the required returned type
     * @param pPluginParameters
     *            an optional list of {@link PluginParameter}
     * 
     * @return an instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> T getPlugin(final List<PluginParameter> pParameters, final Class<T> pReturnInterfaceType,
            final PluginParameter... pPluginParameters) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pReturnInterfaceType);

        final PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", pParameters, 0);
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata, pPluginParameters);
    }

    /**
     *
     * Look for {@link PluginInit} annotation and launch corresponding method if found.
     *
     * @param <T>
     *            a plugin instance
     * @param pPluginInstance
     *            the plugin instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    private static <T> void doInitPlugin(final T pPluginInstance) throws PluginUtilsException {
        final Method[] allMethods = pPluginInstance.getClass().getDeclaredMethods();
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(PluginInit.class)) {
                // Invoke method
                ReflectionUtils.makeAccessible(method);

                try {
                    method.invoke(pPluginInstance);
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error(String.format("Exception while invoking init method on plugin class <%s>.",
                                               pPluginInstance.getClass()),
                                 e);
                    throw new PluginUtilsException(e);
                }
            }
        }
    }

    /**
     *
     * Create an instance of plugin configuration
     *
     * @param <T>
     *            a plugin
     * @param pParameters
     *            the plugin parameters
     * @param pReturnInterfaceType
     *            the required returned type
     * 
     * @return an instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> PluginConfiguration getPluginConfiguration(final List<PluginParameter> pParameters,
            final Class<T> pReturnInterfaceType) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pReturnInterfaceType);

        return new PluginConfiguration(pluginMetadata, "", pParameters, 0);
    }
}
