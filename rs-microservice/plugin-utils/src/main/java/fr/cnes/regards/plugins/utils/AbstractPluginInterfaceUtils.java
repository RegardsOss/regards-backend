/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 *
 * Plugin utilities
 *
 * @author cmertz
 */
public abstract class AbstractPluginInterfaceUtils {

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
     * Retrieve all annotated {@link PluginInterface}.
     * 
     * @param pPrefix
     *            a package prefix used for the search
     * @return all class annotated {@link PluginInterface}
     */
    public static List<String> getInterfaces(String pPrefix) {
        List<String> interfaces = null;

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(PluginInterface.class);

        for (final Class<?> pluginClass : annotatedPlugins) {

            if (interfaces == null) {
                interfaces = new ArrayList<String>();
            }
            interfaces.add(pluginClass.getCanonicalName());
        }

        return interfaces;
    }

}
