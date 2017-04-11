/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.plugins.utils.bean.IPluginUtilsBean;
import fr.cnes.regards.plugins.utils.bean.PluginUtilsBean;

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
     * Interface to be implemented by {@link PluginUtilsBean} to load your own
     * {@link org.springframework.beans.factory.BeanFactory}
     */
    private static IPluginUtilsBean pluginUtilsBean;

    /**
     * Constructor
     */
    private PluginUtils() {
        // Static class
    }

    public static void setPluginUtilsBean(IPluginUtilsBean pPluginUtilsBean) {
        pluginUtilsBean = pPluginUtilsBean;
    }

    /**
     * Retrieve all annotated plugins (@see {@link Plugin}) and initialize a map whose key is the {@link Plugin}
     * identifier and value the required plugin metadata.
     *
     * @param pPrefix a package prefix used for the scan
     * @param pPrefixs a {@link List} of package to scan to find the {@link Plugin} and {@link PluginInterface}
     * @return all class annotated {@link Plugin}
     */
    public static Map<String, PluginMetaData> getPlugins(final String pPrefix, final List<String> pPrefixs) {
        final Map<String, PluginMetaData> plugins = new HashMap<>();

        // Scan class path with Reflections library
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> annotatedPlugins = reflections.getTypesAnnotatedWith(Plugin.class, true);

        // Create a plugin object for each class
        for (final Class<?> pluginClass : annotatedPlugins) {

            // Create plugin metadata
            final PluginMetaData plugin = PluginUtils.createPluginMetaData(pluginClass, pPrefixs);

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
     *
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return all class annotated {@link Plugin}
     */
    public static Map<String, PluginMetaData> getPlugins(final List<String> pPrefixs) {
        final Map<String, PluginMetaData> plugins = new HashMap<>();

        for (final String p : pPrefixs) {
            plugins.putAll(getPlugins(p, pPrefixs));
        }

        return plugins;
    }

    /**
     * Create {@link PluginMetaData} based on its annotations {@link Plugin} and {@link PluginParameters} if any.
     *
     * @param pPluginClass a class that must contains a {@link Plugin} annotation
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return the {@link PluginMetaData} create
     */
    public static PluginMetaData createPluginMetaData(final Class<?> pPluginClass, final List<String> pPrefixs) {
        // Get implementation associated annotations
        final Plugin plugin = pPluginClass.getAnnotation(Plugin.class);

        // Init plugin metadata
        final PluginMetaData pluginMetaData = new PluginMetaData(plugin);
        pluginMetaData.setPluginClassName(pPluginClass.getCanonicalName());

        // Search the plugin type of the plugin class : ie the interface has the @PluginInterface annotation
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(pPrefixs);
        List<String> types = new ArrayList<>();

        for (Class<?> aInterface : pPluginClass.getInterfaces()) {
            types.add(aInterface.getCanonicalName());
            pluginInterfaces.forEach(s -> {
                if (s.equals(aInterface.getCanonicalName())) {
                    pluginMetaData.setInterfaceName(s);
                }
            });
        }

        // Try to detect parameters if any
        pluginMetaData.setParameters(PluginParameterUtils.getParameters(pPluginClass, pPrefixs));

        return pluginMetaData;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     *
     * @param <T> a {@link Plugin}
     * @param pPluginConf the {@link PluginConfiguration}
     * @param pPluginMetadata the {@link PluginMetaData}
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pPluginParameters an optional list of {@link PluginParameter}
     * @return an instance of a {@link Plugin}
     * @throws PluginUtilsException if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(final PluginConfiguration pPluginConf, final PluginMetaData pPluginMetadata,
            final List<String> pPrefixs, final PluginParameter... pPluginParameters) throws PluginUtilsException {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pPluginMetadata.getPluginClassName()).newInstance();

            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, pPluginConf, pPrefixs, pPluginParameters);

            if (pluginUtilsBean != null) {
                pluginUtilsBean.processAutowiredBean(returnPlugin);
            }

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | ClassNotFoundException e) {
            throw new PluginUtilsException(String.format(CANNOT_INSTANTIATE, pPluginMetadata.getPluginClassName()), e);

        }

        return returnPlugin;
    }

    public static <T> T getPlugin(final PluginConfiguration pPluginConf, final PluginMetaData pPluginMetadata,
            final IPluginUtilsBean pPluginUtilsBean, final List<String> pPrefixs,
            final PluginParameter... pPluginParameters) throws PluginUtilsException {
        pluginUtilsBean = pPluginUtilsBean;
        return PluginUtils.getPlugin(pPluginConf, pPluginMetadata, pPrefixs, pPluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and the plugin class name
     *
     * @param <T> a {@link Plugin}
     * @param pPluginConf the {@link PluginConfiguration}
     * @param pPluginClassName the {@link Plugin} class name
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pPluginParameters an optional list of {@link PluginParameter}
     * @return an instance of {@link Plugin}
     * @throws PluginUtilsException if a problem occurs
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPlugin(final PluginConfiguration pPluginConf, final String pPluginClassName,
            final List<String> pPrefixs, final PluginParameter... pPluginParameters) throws PluginUtilsException {
        T returnPlugin = null;

        try {
            // Make a new instance
            returnPlugin = (T) Class.forName(pPluginClassName).newInstance();

            // Post process parameters
            PluginParameterUtils.postProcess(returnPlugin, pPluginConf, pPrefixs, pPluginParameters);

            if (pluginUtilsBean != null) {
                pluginUtilsBean.processAutowiredBean(returnPlugin);
            }

            // Launch init method if detected
            doInitPlugin(returnPlugin);

        } catch (InstantiationException | IllegalAccessException | NoSuchElementException | IllegalArgumentException
                | SecurityException | ClassNotFoundException e) {
            throw new PluginUtilsException(String.format(CANNOT_INSTANTIATE, pPluginClassName), e);
        }

        return returnPlugin;
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     *
     * @param <T> a {@link Plugin}
     * @param pParameters a {@link List} of {@link PluginParameter}
     * @param pReturnInterfaceType the required returned type
     * @param pPluginUtilsBean a {@link PluginUtilsBean} containing your own
     * {@link org.springframework.beans.factory.BeanFactory}
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pPluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance
     * @throws PluginUtilsException if a problem occurs
     */
    public static <T> T getPlugin(final List<PluginParameter> pParameters, final Class<T> pReturnInterfaceType,
            final IPluginUtilsBean pPluginUtilsBean, final List<String> pPrefixs,
            final PluginParameter... pPluginParameters) throws PluginUtilsException {
        pluginUtilsBean = pPluginUtilsBean;
        return PluginUtils.getPlugin(pParameters, pReturnInterfaceType, pPrefixs, pPluginParameters);
    }

    /**
     * Create an instance of {@link Plugin} based on its configuration and metadata
     *
     * @param <T> a {@link Plugin}
     * @param pParameters a {@link List} of {@link PluginParameter}
     * @param pReturnInterfaceType the required returned type
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @param pPluginParameters an optional {@link List} of {@link PluginParameter}
     * @return a {@link Plugin} instance
     * @throws PluginUtilsException if a problem occurs
     */
    public static <T> T getPlugin(final List<PluginParameter> pParameters, final Class<T> pReturnInterfaceType,
            final List<String> pPrefixs, final PluginParameter... pPluginParameters) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pReturnInterfaceType, pPrefixs);

        final PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetadata, "", pParameters);
        return PluginUtils.getPlugin(pluginConfiguration, pluginMetadata, pPrefixs, pPluginParameters);
    }

    /**
     * Look for {@link PluginInit} annotation and launch corresponding method if found.
     *
     * @param <T> a {@link Plugin}
     * @param pPluginInstance the {@link Plugin} instance
     * @throws PluginUtilsException if a problem occurs
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
     * Create an instance of {@link PluginConfiguration}
     *
     * @param <T> a plugin
     * @param pParameters the plugin parameters
     * @param pReturnInterfaceType the required returned type
     * @param pPrefixs a {@link List} of package to scan for find the {@link Plugin} and {@link PluginInterface}
     * @return an instance
     * @throws PluginUtilsException if a problem occurs
     */
    public static <T> PluginConfiguration getPluginConfiguration(final List<PluginParameter> pParameters,
            final Class<T> pReturnInterfaceType, final List<String> pPrefixs) throws PluginUtilsException {
        // Build plugin metadata
        final PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(pReturnInterfaceType, pPrefixs);

        return new PluginConfiguration(pluginMetadata, UUID.randomUUID().toString(), pParameters);
    }

}
