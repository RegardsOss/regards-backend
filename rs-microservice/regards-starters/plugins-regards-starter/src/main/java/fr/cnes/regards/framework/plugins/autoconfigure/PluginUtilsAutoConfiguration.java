/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins.autoconfigure;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.plugins.utils.bean.PluginUtilsBean;

/**
 *
 * Class PluginUtilsAutoConfiguration
 *
 * A bean used to defined a implementation of {@link BeanFactoryAware}.
 *
 * @author Christophe Mertz
 */
@Configuration
@ConditionalOnProperty(prefix = "regards.plugins", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(PluginUtilsProperties.class)
public class PluginUtilsAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public PluginUtilsBean pluginUtilsBean() {
        return new PluginUtilsBean();
    }
}
