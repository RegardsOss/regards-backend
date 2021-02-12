package fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider;

import fr.cnes.regards.framework.gson.annotation.Gsonable;

import java.util.Objects;

@Gsonable(value = "pluginId")
public abstract class ServiceProviderAuthenticationParams {

    private final String serviceProvider;

    protected ServiceProviderAuthenticationParams(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceProviderAuthenticationParams that = (ServiceProviderAuthenticationParams) o;
        return Objects.equals(serviceProvider, that.serviceProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceProvider);
    }
}
