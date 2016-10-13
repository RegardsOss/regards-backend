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
public final class PluginInterfaceUtils {

    /**
     *
     * Constructor
     *
     */
    private PluginInterfaceUtils() {
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
        final List<String> interfaces;

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(PluginInterface.class);

        if (annotatedPlugins.size() > 0) {
            interfaces = new ArrayList<String>();
            annotatedPlugins.stream().forEach(s -> interfaces.add(s.getCanonicalName()));
        } else {
            interfaces = null;
        }

        return interfaces;
    }

}
