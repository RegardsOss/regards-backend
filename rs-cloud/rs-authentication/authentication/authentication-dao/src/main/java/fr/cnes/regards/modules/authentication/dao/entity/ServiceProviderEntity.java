package fr.cnes.regards.modules.authentication.dao.entity;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "t_service_provider")
public class ServiceProviderEntity {

    @Id
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "auth_url", nullable = false)
    private String authUrl;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(
        name = "plugin_conf_id",
        foreignKey = @ForeignKey(name = "fk_service_provider_plugin_conf")
    )
    private PluginConfiguration configuration;

    public ServiceProviderEntity() {}

    public ServiceProviderEntity(String name, String authUrl, PluginConfiguration configuration) {
        this.name = name;
        this.authUrl = authUrl;
        this.configuration = configuration;
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

    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PluginConfiguration configuration) {
        this.configuration = configuration;
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
            && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, configuration);
    }
}
