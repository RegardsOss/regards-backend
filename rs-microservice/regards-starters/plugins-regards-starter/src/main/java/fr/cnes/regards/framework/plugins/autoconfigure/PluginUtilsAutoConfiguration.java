/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins.autoconfigure;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.bean.PluginUtilsBean;

/**
 * Class PluginUtilsAutoConfiguration A bean used to defined a implementation of {@link BeanFactoryAware}.
 *
 * @author Christophe Mertz
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.framework.modules.plugins" })
@AutoConfigureAfter({ MultitenantJpaAutoConfiguration.class, AmqpAutoConfiguration.class })
public class PluginUtilsAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public PluginUtilsBean pluginUtilsBean() {
        PluginUtilsBean pluginUtilsBean = new PluginUtilsBean();
        PluginUtils.setPluginUtilsBean(pluginUtilsBean);
        return pluginUtilsBean;
    }
}
