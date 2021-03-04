/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import feign.Feign;
import feign.FeignException;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.httpclient.ApacheHttpClient;
import fr.cnes.regards.framework.feign.ClientErrorDecoder;
import fr.cnes.regards.framework.feign.ExternalTarget;
import fr.cnes.regards.framework.feign.FeignContractSupplier;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.request.OpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdUserInfoResponse;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.hibernate.validator.constraints.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Base64;
import java.util.NoSuchElementException;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

public abstract class OpenIdConnectPlugin<UserInfoResponse extends OpenIdUserInfoResponse> implements IServiceProviderPlugin<OpenIdAuthenticationParams, OpenIdConnectToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIdConnectPlugin.class);

    public static final String ID = "OpenId";

    public static final String OPENID_CONNECT_TOKEN = "OPENID_CONNECT_TOKEN";

    public static final String OPENID_CLIENT_ID = "OpenId_Client_ID";

    public static final String OPENID_CLIENT_SECRET = "OpenId_Client_Secret";

    public static final String OPENID_TOKEN_ENDPOINT = "OpenId_Token_Endpoint";

    public static final String OPENID_USER_INFO_ENDPOINT = "OpenId_UserInfo_Endpoint";

    public static final String OPENID_REVOKE_ENDPOINT = "OpenId_Revoke_Endpoint";

    @PluginParameter(
        name = OPENID_CLIENT_ID,
        label = "Registered client id for the Service Provider",
        description = "The client id registered for this Service Provider in order to authenticate requests"
    )
    private String clientId;

    @PluginParameter(
        name = OPENID_CLIENT_SECRET,
        label = "Registered client secret for the Service Provider",
        description = "The client secret registered for this Service Provider in order to authenticate requests",
        sensitive = true,
        optional = true
    )
    private String clientSecret;

    @PluginParameter(
        name = OPENID_TOKEN_ENDPOINT,
        label = "Service Provider \"token\" type endpoint",
        description = "The Service Provider endpoint of type \"token\" to authenticate"
    )
    @URL
    private String tokenEndpoint;

    @PluginParameter(
        name = OPENID_USER_INFO_ENDPOINT,
        label = "Service Provider \"userInfo\" type endpoint",
        description = "The Service Provider endpoint of type \"userInfo\" to retrieve info about the authenticated user"
    )
    @URL
    private String userInfoEndpoint;

    @PluginParameter(
        name = OPENID_REVOKE_ENDPOINT,
        label = "Service Provider \"revoke\" type endpoint",
        description = "The Service Provider endpoint of type \"revoke\" to deauthenticate",
        optional = true
    )
    @URL
    private String revokeEndpoint;

    private static final String BASIC_AUTH = "Basic ";

    private static final String BEARER_AUTH = "Bearer ";

    /**
     * HTTP client for external API requests
     */
    @Autowired
    private HttpClient httpClient;

    /**
     * Gson request and response converter
     */
    @Autowired
    private Gson gson;

    private Feign feign;

    @PluginInit
    public void init() {
        feign = Feign.builder()
            .client(new ApacheHttpClient(httpClient))
            .encoder(new FormEncoder())
            .decoder(new ResponseEntityDecoder(new GsonDecoder(gson)))
            .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
            .build();
    }

    @Override
    public Class<OpenIdAuthenticationParams> getAuthenticationParamsType() {
        return OpenIdAuthenticationParams.class;
    }

    @Override
    public Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> authenticate(OpenIdAuthenticationParams params) {
        return token(params)
            .flatMap(token -> userInfo(token)
                .map(userInfo -> new ServiceProviderAuthenticationInfo<>(
                    userInfo.toDomain(),
                    new OpenIdConnectToken(token)
                )));
    }

    @Override
    public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
        //noinspection unchecked
        return jwtClaims.get(OPENID_CONNECT_TOKEN)
            .map(o -> (String) o)
            .toTry()
            .mapFailure(
                Case($(instanceOf(NoSuchElementException.class)), ex -> new InternalAuthenticationServiceException("Missing OpenID Connect token for deauthentication from service provider."))
            )
            .flatMap(this::revoke);
    }

    @Override
    public Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> verify(String token) {
        return userInfo(token)
            .map(userInfo -> new ServiceProviderAuthenticationInfo<>(
                userInfo.toDomain(),
                new OpenIdConnectToken(token)
            ));
    }

    private Try<String> token(OpenIdAuthenticationParams params) {
        String basicString = String.format("%s:%s", clientId, clientSecret);
        basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
        Map<String, String> headers = HashMap.of(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString);
        IOpenIdConnectClient<UserInfoResponse> client = getOauth2Client(tokenEndpoint, headers);

        return Try
            .of(() -> new OpenIdTokenRequest(
                params.getCode(),
                params.getRedirectUri()
            ))
            .map(client::token)
            .transform(this::mapClientException)
            .flatMap(response -> {
                if (response.getStatusCode() != HttpStatus.OK) {
                    return Try.failure(new InternalAuthenticationServiceException(String.format("Service Provider rejected authentication request with status: %s", response.getStatusCode())));
                }
                OpenIdTokenResponse body = response.getBody();
                if (body == null) {
                    return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
                }
                if (!body.getTokenType().equals("bearer")) {
                    return Try.failure(new InsufficientAuthenticationException(String.format("Service Provider returned invalid token type \"%s\", expected \"bearer\".", body.getTokenType())));
                }
                return Try.success(body);
            })
            .map(OpenIdTokenResponse::getAccessToken);
    }

    private Try<OpenIdUserInfoResponse> userInfo(String oauth2Token) {
        Map<String, String> headers = HashMap.of(HttpHeaders.AUTHORIZATION, BEARER_AUTH + oauth2Token);
        IOpenIdConnectClient<UserInfoResponse> client = getOauth2Client(userInfoEndpoint, headers);

        return Try
            .of(client::userInfo)
            .transform(this::mapClientException)
            .flatMap(response -> {
                if (response.getStatusCode() != HttpStatus.OK) {
                    return Try.failure(new InsufficientAuthenticationException(String.format("Service Provider rejected userInfo request with status: %s", response.getStatusCode())));
                }
                OpenIdUserInfoResponse body = response.getBody();
                if (body == null) {
                    return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
                }
                return Try.success(body);
            });
    }

    private Try<Unit> revoke(String oauth2Token) {
        if (! Strings.isNullOrEmpty(revokeEndpoint)) {
            String basicString = String.format("%s:%s", clientId, clientSecret);
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
            Map<String, String> headers = HashMap.of(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString);

            IOpenIdConnectClient<UserInfoResponse> client = getOauth2Client(revokeEndpoint, headers);

            // Execute side effect, and then nothing.
            // It's not like we're going to fail the user's logout operation and ask it to retry?
            // Just let the error fall into limbo.
            Try.run(() -> client.revoke(oauth2Token));
        }
        return Try.success(Unit.UNIT);
    }

    protected IOpenIdConnectClient<UserInfoResponse> getOauth2Client(String url, Map<String, String> headers) {
        return feign.newInstance(
            new ExternalTarget<>(
                getOauth2ClientType(),
                url,
                headers.toJavaMap()
            )
        );
    }

    protected abstract <T extends IOpenIdConnectClient<UserInfoResponse>> Class<T> getOauth2ClientType();

    private <T> Try<T> mapClientException(Try<T> call) {
        //noinspection unchecked
        return call.mapFailure(
            Case($(instanceOf(HttpClientErrorException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex)),
            Case($(instanceOf(HttpServerErrorException.class)), ex -> new AuthenticationServiceException(ex.getMessage(), ex)),
            Case($(instanceOf(FeignException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex))
        );
    }

}
