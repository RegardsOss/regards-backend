package fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.util.NoSuchElementException;
import java.util.Objects;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

@PluginInterface(description = "Interface for all OpenId Connect Service Provider.")
public interface IOpenIdConnectPlugin<OpenIdAuthenticationParams extends ServiceProviderAuthenticationParams>
    extends IServiceProviderPlugin<OpenIdAuthenticationParams, IOpenIdConnectPlugin.OpenIdConnectToken> {

    //TODO make this an enum?
    String OPENID_CONNECT_TOKEN = "OPENID_CONNECT_TOKEN";

    @Override
    default Try<ServiceProviderAuthenticationInfo<IOpenIdConnectPlugin.OpenIdConnectToken>> authenticate(OpenIdAuthenticationParams params) {
        return token(params)
            .flatMap(token -> userInfo(token)
            .map(userInfo -> new ServiceProviderAuthenticationInfo<>(
                userInfo,
                new OpenIdConnectToken(token)
            )));
    }

    @Override
    default Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
        //noinspection unchecked
        return jwtClaims.get(OPENID_CONNECT_TOKEN)
            .map(o -> (String) o)
            .toTry()
            .mapFailure(
                Case($(instanceOf(NoSuchElementException.class)), ex -> new InternalAuthenticationServiceException("Missing OpenID Connect token for deauthentication from service provider."))
            )
            .flatMap(this::revoke);
        //FIXME PM023 revoke REGARDS jwt too
    }

    default Try<String> token(OpenIdAuthenticationParams params) {
        return getOauth2TokenClient().oauth2Token(params);
    }

    default Try<ServiceProviderAuthenticationInfo.UserInfo> userInfo(String oauth2Token) {
        return getOauth2UserInfoClient(oauth2Token).userInfo();
    }

    default Try<Unit> revoke(String oauth2Token) {
        return getOauth2RevokeClient().revoke(oauth2Token);
    }

    IOpenIdConnectClient<OpenIdAuthenticationParams> getOauth2TokenClient();

    IOpenIdConnectClient<OpenIdAuthenticationParams> getOauth2UserInfoClient(String oauth2Token);

    IOpenIdConnectClient<OpenIdAuthenticationParams> getOauth2RevokeClient();

    class OpenIdConnectToken extends ServiceProviderAuthenticationInfo.AuthenticationInfo {
        private final String token;

        public OpenIdConnectToken(String token) {
            this.token = token;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OpenIdConnectToken that = (OpenIdConnectToken) o;
            return Objects.equals(token, that.token);
        }

        @Override
        public int hashCode() {
            return Objects.hash(token);
        }

        @Override
        public String toString() {
            return token;
        }

        @Override
        public Map<String, String> getAuthenticationInfo() {
            return HashMap.of(OPENID_CONNECT_TOKEN, token);
        }
    }
}
