package fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider;

import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Try;

public interface IOpenIdConnectClient<OpenIdAuthenticationParams extends ServiceProviderAuthenticationParams> {

    Try<String> oauth2Token(OpenIdAuthenticationParams params);

    Try<ServiceProviderAuthenticationInfo.UserInfo> userInfo();

    Try<Unit> revoke(String oauth2Token);
}
