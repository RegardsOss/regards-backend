/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.representation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;

/**
 * Configuration class used to initialiaze and set our custom http message converter using the different IRepresentation
 * plugin implementation configured
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class RepresentationConfiguration implements BeanFactoryAware, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Default geo json representation plugin configuration label
     */
    protected static final String DEFAULT_GEO_JSON_CONFIGURATION_LABEL = "Default GeoJSON representation plugin configuration";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RepresentationConfiguration.class);

    /**
     * Bean factory
     */
    private BeanFactory beanFactory;

    /**
     * Plugin service
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * Tenant resolver
     */
    @Autowired
    private ITenantResolver tenantsResolver;

    /**
     * AMQP subscriber
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Allows to configure the representation http message converter
     * @param representationHttpMessageConverter
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    public void configureRepresentationMessageConverter(
            RepresentationHttpMessageConverter representationHttpMessageConverter)
            throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        LOG.info("starting to configure http message converters");
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            tenantResolver.forceTenant(tenant);
            pluginService.addPluginPackage(IRepresentation.class.getPackage().getName());
            pluginService.addPluginPackage(GeoJsonRepresentation.class.getPackage().getName());
            setDefaultPluginConfiguration();
        }
        representationHttpMessageConverter.init(pluginService, tenantResolver, tenantsResolver, subscriber);
    }

    private void setDefaultPluginConfiguration() {
        // lets check if the default configuration is already into the database
        try {
            pluginService.getPluginConfigurationByLabel(DEFAULT_GEO_JSON_CONFIGURATION_LABEL);
            return;
        } catch (EntityNotFoundException e) { // NOSONAR
            // that's ok it just means that the configuration is not there, so we have to create it
        }
        // create a pluginConfiguration for GeoJson
        PluginMetaData geoJsonMeta = PluginUtils.createPluginMetaData(GeoJsonRepresentation.class,
                                                                      Lists.newArrayList(IRepresentation.class
                                                                                                 .getPackage()
                                                                                                 .getName(),
                                                                                         GeoJsonRepresentation.class
                                                                                                 .getPackage()
                                                                                                 .getName()));

        PluginConfiguration geoJsonConf = new PluginConfiguration(geoJsonMeta, DEFAULT_GEO_JSON_CONFIGURATION_LABEL);

        try {
            pluginService.savePluginConfiguration(geoJsonConf);
        } catch (ModuleException e) {
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        RepresentationHttpMessageConverter representationHttpMessageConverter = beanFactory
                .getBean(RepresentationHttpMessageConverter.class);
        try {
            configureRepresentationMessageConverter(representationHttpMessageConverter);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            LOG.error("Could not initialize RepresentationHttpMessageConverter", e);
            Runtime.getRuntime().exit(1);
        }
    }
}
