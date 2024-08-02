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
package fr.cnes.regards.modules.authentication.dao.entity;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "t_service_provider")
public class ServiceProviderEntity {

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "auth_url", nullable = false)
    private String authUrl;

    @Column(name = "logout_url")
    private String logoutUrl;

    @Column(name = "description_fr", length = 255)
    private String descriptionFr;

    @Column(name = "description_en", length = 255)
    private String descriptionEn;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plugin_conf_id", foreignKey = @ForeignKey(name = "fk_service_provider_plugin_conf"))
    private PluginConfiguration configuration;

    public ServiceProviderEntity() {
    }

    public ServiceProviderEntity(String name,
                                 String authUrl,
                                 String logoutUrl,
                                 PluginConfiguration configuration,
                                 String descriptionFr,
                                 String descriptionEn) {
        this.name = name;
        this.authUrl = authUrl;
        this.logoutUrl = logoutUrl;
        this.configuration = configuration;
        this.descriptionFr = descriptionFr;
        this.descriptionEn = descriptionEn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PluginConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public void setDescriptionFr(String descriptionFr) {
        this.descriptionFr = descriptionFr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceProviderEntity that = (ServiceProviderEntity) o;
        return Objects.equals(name, that.name)
               && Objects.equals(authUrl, that.authUrl)
               && Objects.equals(configuration,
                                 that.configuration)
               && Objects.equals(descriptionEn, that.descriptionEn)
               && Objects.equals(descriptionFr, that.descriptionFr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, configuration, descriptionFr, descriptionEn);
    }
}
