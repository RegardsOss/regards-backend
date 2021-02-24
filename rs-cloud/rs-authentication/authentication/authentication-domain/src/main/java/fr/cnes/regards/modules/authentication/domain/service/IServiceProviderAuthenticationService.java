package fr.cnes.regards.modules.authentication.domain.service;

import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Try;

public interface IServiceProviderAuthenticationService {

    Try<String> authenticate(String serviceProviderName, ServiceProviderAuthenticationParams params);

    Try<Unit> deauthenticate(String serviceProviderName);

    Try<String> verifyAndAuthenticate(String externalToken);
}
