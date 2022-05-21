/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.domain.data;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

import java.util.Objects;

public class ServiceProvider {

    private final String name;

    private final String authUrl;

    private final String logoutUrl;

    private final PluginConfiguration configuration;

    public ServiceProvider(String name, String authUrl, String logoutUrl, PluginConfiguration configuration) {
        this.name = name;
        this.authUrl = authUrl;
        this.logoutUrl = logoutUrl;
        this.configuration = configuration;
    }

    public String getName() {
        return name;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceProvider that = (ServiceProvider) o;
        return Objects.equals(name, that.name) && Objects.equals(authUrl, that.authUrl) && Objects.equals(configuration,
                                                                                                          that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, configuration);
    }
}
