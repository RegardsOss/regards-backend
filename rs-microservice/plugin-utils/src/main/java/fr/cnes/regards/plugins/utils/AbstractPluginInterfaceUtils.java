/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;

/**
 *
 * Plugin utilities
 *
 * @author cmertz
 */
public abstract class AbstractPluginInterfaceUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPluginInterfaceUtils.class);

    /**
     *
     * Constructor
     *
     */
    private AbstractPluginInterfaceUtils() {
        // Static class
    }

    /**
     *
     * Retrieve all annotated plugininterface (@see {@link PluginInterface}) and init a map whose key is the plugin
     * identifier and value the required plugin metadata.
     * 
     * @param pPrefix
     *            a package prefix used for the search
     * @return all class annotated {@link PluginInterface}
     * @throws PluginUtilsException
     *             a pluginId is found a twice
     */
    public static List<String> getInterfaces(String pPrefix) {
        List<String> interfaces = null;

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(PluginInterface.class);

//        // Create a plugin object for each class
//        for (final Class<?> pluginClass : annotatedPlugins) {
//pluginClass.g
//            // Check a plugin does not already exists with the same id
//            if (plugins.containsKey(plugin.getId())) {
//                final PluginMetaData pMeta = plugins.get(plugin.getId());
//                final String message = String.format(
//                                                     "Plugin identifier must be unique : %s for plugin \"%s\" already used in plugin \"%s\"!",
//                                                     plugin.getId(), plugin.getPluginClass(), pMeta.getPluginClass());
//                throw new PluginUtilsException(message);
//            }
//
//            // Store plugin reference
//            plugins.put(plugin.getMetaDataId(), plugin);
//            LOGGER.info(String.format("Plugin \"%s\" with identifier \"%s\" loaded.",
//                                      plugin.getPluginClass().getTypeName(), plugin.getId()));
//        }
        return interfaces;
    }


}
