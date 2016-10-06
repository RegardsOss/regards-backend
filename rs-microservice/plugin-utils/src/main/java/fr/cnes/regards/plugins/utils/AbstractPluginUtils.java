/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;

/**
 *
 * Plugin utilities
 *
 * @author cmertz
 */
public abstract class AbstractPluginUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPluginUtils.class);

    /**
     *
     * Constructor
     *
     */
    private AbstractPluginUtils() {
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
    public static Map<String, PluginMetaData> getPlugins(String pPrefix) throws PluginUtilsException {
// OK
        final Map<String, PluginMetaData> plugins = new HashMap<String, PluginMetaData>();

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(Plugin.class);

        // Create a plugin object for each class
        for (final Class<?> pluginClass : annotatedPlugins) {

            // Create plugin metadata
            final PluginMetaData plugin = AbstractPluginUtils.createPluginMetaData(pluginClass);

            // Check a plugin does not already exists with the same id
            if (plugins.containsKey(plugin.getId())) {
                final PluginMetaData pMeta = plugins.get(plugin.getId());
                final String message = String.format(
                                                     "Plugin identifier must be unique : %s for plugin \"%s\" already used in plugin \"%s\"!",
                                                     plugin.getId(), plugin.getPluginClass(), pMeta.getPluginClass());
                throw new PluginUtilsException(message);
            }

            // Store plugin reference
            plugins.put(plugin.getMetaDataId(), plugin);
            LOGGER.info(String.format("Plugin \"%s\" with identifier \"%s\" loaded.",
                                      plugin.getPluginClass().getTypeName(), plugin.getId()));
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
    public static PluginMetaData createPluginMetaData(Class<?> pPluginClass) {
// OK
        PluginMetaData pluginMetaData = null;

        // Get implementation associated annotations
        final Plugin plugin = pPluginClass.getAnnotation(Plugin.class);

        // Init plugin metadata
        pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(pPluginClass);
        
        // Manage plugin id
        if ("".equals(plugin.id())) {
            pluginMetaData.setMetaDataId(pPluginClass.getCanonicalName());
        } else {
            pluginMetaData.setMetaDataId(plugin.id());
        }
        pluginMetaData.setAuthor(plugin.author());
        pluginMetaData.setVersion(plugin.version());
        pluginMetaData.setDescription(plugin.description());
        
        // Try to detect parameters if any
        pluginMetaData.setParameters(AbstractPluginParametersUtil.getParameters(pPluginClass));

        return pluginMetaData;
    }

    /**
     *
     * Create an instance of plugin based on its configuration and metadata
     *
     * @param pPluginConf
     *            the plugin configuration
     * @param pPluginMetadata
     *            the plugin metadata
     * @return an instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> T getPlugin(PluginConfiguration pPluginConf, PluginMetaData pPluginMetadata)
            throws PluginUtilsException {
// OK
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) pPluginMetadata.getPluginClass().newInstance();
            
            // Post process parameters
            AbstractPluginParametersUtil.postProcess(returnPlugin, pPluginConf);
            
            // Launch init method if detected
            doInitPlugin(returnPlugin);
            
        } catch (InstantiationException | IllegalAccessException e) {
            throw new PluginUtilsException(String.format("Cannot instantiate \"%s\"", pPluginMetadata.getPluginClass()),
                    e);
        }

        return returnPlugin;
    }

    /**
     *
     * Look for {@link PluginInit} annotation and launch corresponding method if found.
     *
     * @param pPluginInstance
     *            the plugin instance
     * @throws PluginUtilsException
     *             if problem occurs
     */
    public static <T> void doInitPlugin(T pPluginInstance) throws PluginUtilsException {
        final Method[] allMethods = pPluginInstance.getClass().getDeclaredMethods();
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(PluginInit.class)) {
                // Invoke method
                AbstractReflectionUtils.makeAccessible(method);
                
                try {
                    method.invoke(pPluginInstance);
                } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    LOGGER.error(String.format("Exception while invoking init method on plugin class \"%s\".",
                                               pPluginInstance.getClass()),
                                 e);
                    throw new PluginUtilsException(e);
                }
            }
        }
    }
}
