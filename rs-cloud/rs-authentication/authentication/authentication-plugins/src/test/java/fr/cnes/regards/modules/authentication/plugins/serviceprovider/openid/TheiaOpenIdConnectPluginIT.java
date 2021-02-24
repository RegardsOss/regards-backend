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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.TheiaOpenIdConnectPlugin;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response.TheiaOpenIdUserInfoResponse;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=theia_authentication_service_provider_tests",
    })
public class TheiaOpenIdConnectPluginIT extends AbstractRegardsServiceIT {

    public static final String ENDPOINT_FORMAT = "http://localhost:%s%s";
    public static final String TOKEN_ENDPOINT = "/token";
    public static final String USER_INFO_ENDPOINT = "/userInfo";
    public static final String REVOKE_ENDPOINT = "/revoke";
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        PluginUtils.setup();
    }

    private TheiaOpenIdConnectPlugin getPlugin() {
        try {
            // Set all parameters
            Set<IPluginParam> parameters = IPluginParam
                .set(
                    IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_ID, "I don't feel like dancin'"),
                    IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_SECRET, encryptionService.encrypt("Rather be home with no-one if I can't get down with you-ou-ou")),
                    IPluginParam.build(OpenIdConnectPlugin.OPENID_TOKEN_ENDPOINT, String.format(ENDPOINT_FORMAT, wireMockRule.port(), TOKEN_ENDPOINT)),
                    IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_ENDPOINT, String.format(ENDPOINT_FORMAT, wireMockRule.port(), USER_INFO_ENDPOINT)),
                    IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT, String.format(ENDPOINT_FORMAT, wireMockRule.port(), REVOKE_ENDPOINT))
                );

            PluginConfiguration conf = PluginConfiguration.build(TheiaOpenIdConnectPlugin.class, "", parameters);
            TheiaOpenIdConnectPlugin plugin = PluginUtils.getPlugin(conf, new HashMap<>());
            Assert.assertNotNull(plugin);
            return plugin;
        } catch (Exception e) {
            Assert.fail();
            return null; // never reached, dummy
        }
    }

    @Test
    public void shouldReturnRightPluginType() {
        assertThat(getPlugin()).isInstanceOf(OpenIdConnectPlugin.class);
    }

    @Test
    public void authenticate_fails_when_token_request_client_fails() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(400)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("code", "uri"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InternalAuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=code&grant_type=authorization_code&redirect_uri=uri"))
        );
    }

    @Test
    public void authenticate_fails_when_token_request_server_fails() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(500)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("code", "uri"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(AuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=code&grant_type=authorization_code&redirect_uri=uri"))
        );
    }

    @Test
    public void authenticate_fails_when_token_request_fails_unexpectedly() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(300)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("code", "uri"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InternalAuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=code&grant_type=authorization_code&redirect_uri=uri"))
        );
    }

    @Test
    public void authenticate_fails_when_token_request_returns_empty_body() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("code", "uri"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(AuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=code&grant_type=authorization_code&redirect_uri=uri"))
        );
    }

    @Test
    public void authenticate_fails_when_token_request_returns_wrong_status_code() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(204)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("code", "uri"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InternalAuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=code&grant_type=authorization_code&redirect_uri=uri"))
        );
    }

    @Test
    public void authenticate_fails_when_token_request_returns_wrong_token_type() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse("basic", 10L, "foo", "bar")))));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("code", "uri"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InsufficientAuthenticationException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=code&grant_type=authorization_code&redirect_uri=uri"))
        );
    }

    @Test
    public void authenticate_fails_when_userInfo_request_client_fails() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse("bearer", 10L, "foo", "bar")))));
        stubFor(
            get(urlEqualTo(USER_INFO_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(400)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("foo", "bar"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InternalAuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=foo&grant_type=authorization_code&redirect_uri=bar"))
        );
        verify(
            getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        );
    }

    @Test
    public void authenticate_fails_when_userInfo_request_server_fails() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse("bearer", 10L, "foo", "bar")))));
        stubFor(
            get(urlEqualTo(USER_INFO_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(500)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("foo", "bar"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(AuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=foo&grant_type=authorization_code&redirect_uri=bar"))
        );
        verify(
            getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        );
    }

    @Test
    public void authenticate_fails_when_userInfo_request_fails_unexpectedly() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse("bearer", 10L, "foo", "bar")))));
        stubFor(
            get(urlEqualTo(USER_INFO_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(300)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("foo", "bar"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InternalAuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=foo&grant_type=authorization_code&redirect_uri=bar"))
        );
        verify(
            getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        );
    }

    @Test
    public void authenticate_fails_when_userInfo_request_returns_wrong_status_code() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse("bearer", 10L, "foo", "bar")))));
        stubFor(
            get(urlEqualTo(USER_INFO_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(204)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("foo", "bar"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(InsufficientAuthenticationException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=foo&grant_type=authorization_code&redirect_uri=bar"))
        );
        verify(
            getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        );
    }

    @Test
    public void authenticate_fails_when_userInfo_request_returns_empty_body() {
        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse("bearer", 10L, "foo", "bar")))));
        stubFor(
            get(urlEqualTo(USER_INFO_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("foo", "bar"));

        assertThat(userInfo.isFailure()).isTrue();
        assertThat(userInfo.getCause()).isExactlyInstanceOf(AuthenticationServiceException.class);

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=foo&grant_type=authorization_code&redirect_uri=bar"))
        );
        verify(
            getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        );
    }

    @Test
    public void authenticate_succeeds_when_all_is_well() {
        String tokenType = "bearer";
        long expiresIn = 10L;
        String refreshToken = "refreshToken";
        String accessToken = "accessToken";

        stubFor(
            post(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(new OpenIdTokenResponse(tokenType, expiresIn, refreshToken, accessToken)))));

        String
            regDate = "regDate",
            role = "role",
            ignAuthentication = "ignAuthentication",
            ignKey = "ignKey",
            country = "country",
            source = "source",
            streetAddress = "streetAddress",
            telephone = "telephone",
            type = "type",
            function = "function",
            organization = "organization",
            lastname = "lastname",
            firstname = "firstname",
            email = "email";

        TheiaOpenIdUserInfoResponse response =
            new TheiaOpenIdUserInfoResponse(
                email,
                firstname,
                lastname,
                organization,
                function,
                type,
                telephone,
                streetAddress,
                source,
                country,
                ignKey,
                ignAuthentication,
                role,
                regDate);

        stubFor(
            get(urlEqualTo(USER_INFO_ENDPOINT))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(gson.toJson(response))));

        //noinspection ConstantConditions: getPlugin is guaranteed to return a non-null result, stupid IDE...
        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> userInfo =
            getPlugin().authenticate(new OpenIdAuthenticationParams("foo", "bar"));

        assertThat(userInfo.isSuccess()).isTrue();
        assertThat(userInfo.get())
            .isEqualTo(new ServiceProviderAuthenticationInfo<>(
                new ServiceProviderAuthenticationInfo.UserInfo.Builder()
                    .withEmail(email)
                    .withFirstname(firstname)
                    .withLastname(lastname)
                    .addMetadata("organization", organization)
                    .addMetadata("function", function)
                    .addMetadata("type", type)
                    .addMetadata("telephone", telephone)
                    .addMetadata("streetAddress", streetAddress)
                    .addMetadata("source", source)
                    .addMetadata("country", country)
                    .addMetadata("ignKey", ignKey)
                    .addMetadata("ignAuthentication", ignAuthentication)
                    .addMetadata("role", role)
                    .addMetadata("regDate", regDate)
                    .build(),
                new OpenIdConnectToken(accessToken)
            ));

        verify(
            postRequestedFor(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .withRequestBody(equalTo("code=foo&grant_type=authorization_code&redirect_uri=bar"))
        );
        verify(
            getRequestedFor(urlEqualTo(USER_INFO_ENDPOINT))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        );
    }

    @Test
    public void deauthenticate_fails_when_claim_does_not_contain_openid_token() {
        //noinspection ConstantConditions
        Try<Unit> result = getPlugin().deauthenticate(io.vavr.collection.HashMap.empty());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isExactlyInstanceOf(InternalAuthenticationServiceException.class);
    }

    @Test
    public void deauthenticate_succeeds_when_claim_contains_openid_token() {
        //noinspection ConstantConditions
        Try<Unit> result = getPlugin().deauthenticate(io.vavr.collection.HashMap.of(OpenIdConnectPlugin.OPENID_CONNECT_TOKEN, "foo"));

        assertThat(result.isSuccess()).isTrue();
    }
}
