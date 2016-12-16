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
 * @author Christophe Mertz
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
     * @param pPrefixs
     *            a list of package prefix used for the search
     * @return all class annotated {@link PluginInterface}
     */
    public static List<String> getInterfaces(final List<String> pPrefixs) {
        final List<String> interfaces = new ArrayList<>();

        pPrefixs.forEach(p -> {
            final List<String> ll = getInterfaces(p);
            if (ll != null && !ll.isEmpty()) {
                ll.forEach(s -> interfaces.add(s));
            }
        });

        return interfaces;
    }

    /**
     *
     * Retrieve all annotated {@link PluginInterface}.
     * 
     * @param pPrefix
     *            a package prefix used for the search
     * @return all class annotated {@link PluginInterface}
     */
    public static List<String> getInterfaces(final String pPrefix) {
        final List<String> interfaces = new ArrayList<>();

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(PluginInterface.class, true);

        if (!annotatedPlugins.isEmpty()) {
            annotatedPlugins.stream().forEach(str -> interfaces.add(str.getCanonicalName()));
        }

        return interfaces;
    }

}
