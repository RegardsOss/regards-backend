package fr.cnes.regards.modules.authentication.domain.data;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

import java.util.Objects;

public class ServiceProvider {

    private final String name;

    private final String authUrl;

    private final PluginConfiguration configuration;

    public ServiceProvider(String name, String authUrl, PluginConfiguration configuration) {
        this.name = name;
        this.authUrl = authUrl;
        this.configuration = configuration;
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
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        ServiceProvider that = (ServiceProvider) o;
        return Objects.equals(name, that.name)
            && Objects.equals(authUrl, that.authUrl)
            && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authUrl, configuration);
    }
}
