/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins.autoconfigure;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.bean.PluginUtilsBean;

/**
 * Class PluginUtilsAutoConfiguration A bean used to defined a implementation of {@link BeanFactoryAware}.
 *
 * @author Christophe Mertz
 */
@Configuration
@EnableConfigurationProperties(PluginUtilsProperties.class)
@ComponentScan(basePackages = { "fr.cnes.regards.framework.modules.plugins" })
@AutoConfigureAfter({ MultitenantJpaAutoConfiguration.class, AmqpAutoConfiguration.class })
public class PluginUtilsAutoConfiguration {

    /**
     * The attribute represents the plugin's properties
     */
    @Autowired
    private PluginUtilsProperties pluginUtilsProperties;

    @ConditionalOnMissingBean
    @Bean
    public PluginUtilsBean pluginUtilsBean() {
        PluginUtilsBean pluginUtilsBean = new PluginUtilsBean();
        PluginUtils.setPluginUtilsBean(pluginUtilsBean);
        return pluginUtilsBean;
    }

    @Bean
    public IPluginService pluginService(IPluginConfigurationRepository pPluginConfRepo, IPublisher publisher) {
        return new PluginService(pPluginConfRepo, pluginUtilsProperties.getPackagesToScan(), publisher);
    }

}
