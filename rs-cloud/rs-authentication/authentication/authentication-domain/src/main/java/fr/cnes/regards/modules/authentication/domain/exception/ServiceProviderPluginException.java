package fr.cnes.regards.modules.authentication.domain.exception;

public class ServiceProviderPluginException extends RuntimeException {

    public ServiceProviderPluginException(String message) {
        super(message);
    }

    public ServiceProviderPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
