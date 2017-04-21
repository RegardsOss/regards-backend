/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.representation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.search.domain.IRepresentation;
import fr.cnes.regards.plugins.utils.PluginUtils;

/**
 * Configuration class used to initialiaze and set our custom http message converter using the different IRepresentation
 * plugin implementation configured
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Configuration
public class RepresentationConfiguration extends WebMvcConfigurerAdapter {

    private static final String DEFAULT_GEO_JSON_CONFIGURATION_LABEL = "Default GeoJSON representation plugin configuration";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ITenantResolver tenantsResolver;

    @Autowired
    private RepresentationHttpMessageConverter representationHttpMessageConverter;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            tenantResolver.forceTenant(tenant);
            pluginService.addPluginPackage(IRepresentation.class.getPackage().getName());
            pluginService.addPluginPackage(GeoJsonRepresentation.class.getPackage().getName());
            setDefaultPluginConfiguration();
        }
        super.configureMessageConverters(converters);
        converters.add(representationHttpMessageConverter);

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
        PluginMetaData geoJsonMeta = PluginUtils
                .createPluginMetaData(GeoJsonRepresentation.class,
                                      Lists.newArrayList(IRepresentation.class.getPackage().getName(),
                                                         GeoJsonRepresentation.class.getPackage().getName()));

        PluginConfiguration geoJsonConf = new PluginConfiguration(geoJsonMeta, DEFAULT_GEO_JSON_CONFIGURATION_LABEL);

        try {
            pluginService.savePluginConfiguration(geoJsonConf);
        } catch (ModuleException e) {
            throw new RuntimeException(e);// NOSONAR: developpment exception, should not be thrown!
        }
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(GeoJsonRepresentation.MEDIA_TYPE);
    }
}
