/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.rest.dto;

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

    private final PluginConfiguration configuration;

    public ServiceProviderDto(String name, String authUrl, PluginConfiguration configuration) {
        this.name = name;
        this.authUrl = authUrl;
        this.configuration = configuration;
    }

    public ServiceProviderDto(ServiceProvider serviceProvider) {
        this.name = serviceProvider.getName();
        this.authUrl = serviceProvider.getAuthUrl();
        this.configuration = serviceProvider.getConfiguration();
    }

    public String getName() {
        return name;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceProviderDto that = (ServiceProviderDto) o;
        return Objects.equals(name, that.name)
            && Objects.equals(authUrl, that.authUrl)
            && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, configuration);
    }

    public ServiceProvider toDomain() {
        return new ServiceProvider(
            name,
            authUrl,
            configuration
        );
    }
}
