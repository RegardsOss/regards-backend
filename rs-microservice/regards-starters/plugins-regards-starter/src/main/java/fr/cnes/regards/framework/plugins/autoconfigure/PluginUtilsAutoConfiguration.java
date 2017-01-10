/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins.autoconfigure;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
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
@EnableConfigurationProperties(PluginUtilsProperties.class)
@ComponentScan(basePackages = { "fr.cnes.regards.framework.modules.plugins" })
public class PluginUtilsAutoConfiguration {

    @Autowired
    PluginUtilsProperties pluginUtilsProperties;

    @ConditionalOnMissingBean
    @Bean
    public PluginUtilsBean pluginUtilsBean() {
        return new PluginUtilsBean();
    }

    @Bean
    public IPluginService pluginService(IPluginConfigurationRepository pluginConfRepo) {
        return new PluginService(pluginConfRepo, pluginUtilsProperties.getPackagesToScan());
    }

}
