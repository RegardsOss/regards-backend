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

import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.data.validator.ServiceProviderName;
import org.hibernate.validator.constraints.URL;

import java.util.Objects;

/**
 * Service provider dto used by public interfaces
 *
 * @author Théo Lasserre
 */
public class ServiceProviderPublicDto {

    @ServiceProviderName
    private final String name;

    @URL
    private final String authUrl;

    private final String pluginId;

    private final String descriptionFr;

    private final String descriptionEn;

    public ServiceProviderPublicDto(String name,
                                    String authUrl,
                                    String pluginId,
                                    String descriptionEn,
                                    String descriptionFr) {
        this.name = name;
        this.authUrl = authUrl;
        this.pluginId = pluginId;
        this.descriptionFr = descriptionFr;
        this.descriptionEn = descriptionEn;
    }

    public ServiceProviderPublicDto(ServiceProvider serviceProvider) {
        this.name = serviceProvider.getName();
        this.authUrl = serviceProvider.getAuthUrl();
        this.pluginId = serviceProvider.getConfiguration().getPluginId();
        this.descriptionEn = serviceProvider.getDescriptionEn();
        this.descriptionFr = serviceProvider.getDescriptionFr();
    }

    public String getName() {
        return name;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceProviderPublicDto that = (ServiceProviderPublicDto) o;
        return Objects.equals(name, that.name)
               && Objects.equals(authUrl, that.authUrl)
               && Objects.equals(pluginId,
                                 that.pluginId)
               && Objects.equals(descriptionEn, that.descriptionEn)
               && Objects.equals(descriptionFr, that.descriptionFr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, pluginId, descriptionFr, descriptionEn);
    }
}
