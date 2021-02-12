package fr.cnes.regards.modules.authentication.domain.exception.serviceprovider;

import fr.cnes.regards.modules.authentication.domain.exception.ServiceProviderPluginException;

public class ServiceProviderPluginIllegalParameterException extends ServiceProviderPluginException {

    public ServiceProviderPluginIllegalParameterException(String message) {
        super(message);
    }

    public ServiceProviderPluginIllegalParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
