/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.domain.dto;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.data.validator.ServiceProviderName;
import org.hibernate.validator.constraints.URL;

import java.util.Objects;

public class ServiceProviderDto {

    @ServiceProviderName
    private final String name;

    @URL
    private final String authUrl;

    @URL
    private final String logoutUrl;

    private final PluginConfiguration pluginConfiguration;

    private final String descriptionFr;

    private final String descriptionEn;

    public ServiceProviderDto(String name,
                              String authUrl,
                              String logoutUrl,
                              PluginConfiguration pluginConfiguration,
                              String descriptionEn,
                              String descriptionFr) {
        this.name = name;
        this.authUrl = authUrl;
        this.logoutUrl = logoutUrl;
        this.pluginConfiguration = pluginConfiguration;
        this.descriptionEn = descriptionEn;
        this.descriptionFr = descriptionFr;
    }

    public ServiceProviderDto(ServiceProvider serviceProvider) {
        this.name = serviceProvider.getName();
        this.authUrl = serviceProvider.getAuthUrl();
        this.logoutUrl = serviceProvider.getLogoutUrl();
        this.pluginConfiguration = serviceProvider.getConfiguration();
        this.descriptionFr = serviceProvider.getDescriptionFr();
        this.descriptionEn = serviceProvider.getDescriptionEn();
    }

    public String getName() {
        return name;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceProviderDto that = (ServiceProviderDto) o;
        return Objects.equals(name, that.name) && Objects.equals(authUrl, that.authUrl) && Objects.equals(
            pluginConfiguration,
            that.pluginConfiguration) && Objects.equals(descriptionEn, that.descriptionEn) && Objects.equals(
            descriptionFr,
            that.descriptionFr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, pluginConfiguration, descriptionFr, descriptionEn);
    }

    public ServiceProvider toDomain() {
        return new ServiceProvider(name, authUrl, logoutUrl, pluginConfiguration, descriptionFr, descriptionEn);
    }
}
