/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import feign.Feign;
import feign.FeignException;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.httpclient.ApacheHttpClient;
import fr.cnes.regards.framework.feign.ClientErrorDecoder;
import fr.cnes.regards.framework.feign.ExternalTarget;
import fr.cnes.regards.framework.feign.FeignContractSupplier;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.IOpenIdConnectClient;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.IOpenIdConnectPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.TheiaAuthenticationParams;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.request.TheiaOpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdUserInfoResponse;
import io.vavr.collection.HashMap;
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
import java.util.Map;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

@Plugin(
    id = TheiaOpenIdConnectPlugin.ID,
    author = "REGARDS Team",
    description = "Plugin handling the authentication via Theia OpenId Service Provider",
    version = TheiaOpenIdConnectPlugin.VERSION,
    contact = "regards@c-s.fr",
    license = "GPLv3",
    owner = "CNES",
    url = "https://regardsoss.github.io/"
)
public class TheiaOpenIdConnectPlugin implements IOpenIdConnectPlugin<TheiaAuthenticationParams> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TheiaOpenIdConnectPlugin.class);

    public static final String ID = "TheiaOpenId";

    public static final String VERSION = "1.0";

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
        sensitive = true
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
        description = "The Service Provider endpoint of type \"revoke\" to deauthenticate"
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
        //FIXME PM023 if params type not @Gsonable or cannot use type field maybe register type with gson builder (to autowire) here

        feign = Feign.builder()
            .client(new ApacheHttpClient(httpClient))
            .encoder(new FormEncoder())
            .decoder(new ResponseEntityDecoder(new GsonDecoder(gson)))
            .errorDecoder(new ClientErrorDecoder()).decode404().contract(new FeignContractSupplier().get())
            .build();
    }

    @Override
    public Class<TheiaAuthenticationParams> getAuthenticationParamsType() {
        return TheiaAuthenticationParams.class;
    }

    @Override
    public DelegatedOpenIdConnectClient getOauth2TokenClient() {
        String basicString = String.format("%s:%s", clientId, clientSecret);
        basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
        Map<String, String> headers = ImmutableMap.of(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString);

        return getOauth2Client(tokenEndpoint, headers);
    }

    @Override
    public DelegatedOpenIdConnectClient getOauth2UserInfoClient(String oauth2Token) {
        Map<String, String> headers = ImmutableMap.of(HttpHeaders.AUTHORIZATION, BEARER_AUTH + oauth2Token);

        return getOauth2Client(userInfoEndpoint, headers);
    }

    @Override
    public IOpenIdConnectClient<TheiaAuthenticationParams> getOauth2RevokeClient() {
        String basicString = String.format("%s:%s", clientId, clientSecret);
        basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
        Map<String, String> headers = ImmutableMap.of(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString);

        return getOauth2Client(revokeEndpoint, headers);
    }

    private DelegatedOpenIdConnectClient getOauth2Client(@URL String url, Map<String, String> headers) {
        return new DelegatedOpenIdConnectClient(
            feign.newInstance(
                new ExternalTarget<>(
                    TheiaOpenIdConnectClient.class,
                    url,
                    headers
                )
            )
        );
    }

    public static class DelegatedOpenIdConnectClient implements IOpenIdConnectClient<TheiaAuthenticationParams> {

        private final TheiaOpenIdConnectClient delegate;

        public DelegatedOpenIdConnectClient(TheiaOpenIdConnectClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public Try<String> oauth2Token(TheiaAuthenticationParams theiaAuthenticationParams) {
            return Try
                .of(() -> new TheiaOpenIdTokenRequest(
                    theiaAuthenticationParams.getCode(),
                    theiaAuthenticationParams.getRedirectUri()
                ))
                .map(delegate::token)
                .transform(this::mapClientException)
                .flatMap(response -> {
                    if (response.getStatusCode() != HttpStatus.OK) {
                        return Try.failure(new InternalAuthenticationServiceException(String.format("Service Provider rejected authentication request with status: %s", response.getStatusCode())));
                    }
                    TheiaOpenIdTokenResponse body = response.getBody();
                    if (body == null) {
                        return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
                    }
                    if (!body.getTokenType().equals("bearer")) {
                        return Try.failure(new InsufficientAuthenticationException(String.format("Service Provider returned invalid token type \"%s\", expected \"bearer\".", body.getTokenType())));
                    }
                    return Try.success(body);
                })
                .map(TheiaOpenIdTokenResponse::getAccessToken);
        }

        @Override
        public Try<ServiceProviderAuthenticationInfo.UserInfo> userInfo() {
            return Try
                .of(delegate::userInfo)
                .transform(this::mapClientException)
                .flatMap(response -> {
                    if (response.getStatusCode() != HttpStatus.OK) {
                        return Try.failure(new InsufficientAuthenticationException(String.format("Service Provider rejected userInfo request with status: %s", response.getStatusCode())));
                    }
                    TheiaOpenIdUserInfoResponse body = response.getBody();
                    if (body == null) {
                        return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
                    }
                    return Try.success(body);
                })
                .map(response -> new ServiceProviderAuthenticationInfo.UserInfo.Builder()
                    .withEmail(response.getEmail())
                    .withFirstname(response.getFirstname())
                    .withLastname(response.getLastname())
                    .addMetadata("organization", response.getOrganization())
                    .addMetadata("function", response.getFunction())
                    .addMetadata("type", response.getType())
                    .addMetadata("telephone", response.getTelephone())
                    .addMetadata("streetAddress", response.getStreetAddress())
                    .addMetadata("source", response.getSource())
                    .addMetadata("country", response.getCountry())
                    .addMetadata("ignKey", response.getIgnKey())
                    .addMetadata("ignAuthentication", response.getIgnAuthentication())
                    .addMetadata("role", response.getRole())
                    .addMetadata("regDate", response.getRegDate())
                    .build()
                );
        }

        @Override
        public Try<Unit> revoke(String oauth2Token) {
            // No token revoke endpoint for Theia Service Provider
            // No-op
            return Try.success(Unit.UNIT);
        }

        private <T> Try<T> mapClientException(Try<T> call) {
            //noinspection unchecked
            return call.mapFailure(
                Case($(instanceOf(HttpClientErrorException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex)),
                Case($(instanceOf(HttpServerErrorException.class)), ex -> new AuthenticationServiceException(ex.getMessage(), ex)),
                Case($(instanceOf(FeignException.class)), ex -> new InternalAuthenticationServiceException(ex.getMessage(), ex))
            );
        }
    }
}
