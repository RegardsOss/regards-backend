/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.bean.PluginUtilsBean;

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
