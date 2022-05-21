/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.request.OpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdTokenResponse;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.hibernate.validator.constraints.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.net.ssl.SSLContext;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;

@Plugin(id = OpenIdConnectPlugin.ID, author = "REGARDS Team",
    description = "Plugin handling the authentication via OpenId Service Provider",
    version = OpenIdConnectPlugin.VERSION, contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
    url = "https://regardsoss.github.io/")
public class OpenIdConnectPlugin implements IServiceProviderPlugin<OpenIdAuthenticationParams, OpenIdConnectToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIdConnectPlugin.class);

    public static final String ID = "OpenId";

    public static final String VERSION = "1.0";

    public static final String OPENID_CONNECT_TOKEN = "OPENID_CONNECT_TOKEN";

    public static final String OPENID_CLIENT_ID = "OpenId_Client_ID";

    public static final String OPENID_CLIENT_SECRET = "OpenId_Client_Secret";

    public static final String OPENID_REDIRECT_URI = "OpenId_Redirect_Uri";

    public static final String OPENID_TOKEN_ENDPOINT = "OpenId_Token_Endpoint";

    public static final String OPENID_USER_INFO_ENDPOINT = "OpenId_UserInfo_Endpoint";

    public static final String OPENID_USER_INFO_EMAIL_MAPPING = "OpenId_UserInfo_Email_Mapping";

    public static final String OPENID_USER_INFO_FIRSTNAME_MAPPING = "OpenId_UserInfo_Firstname_Mapping";

    public static final String OPENID_USER_INFO_LASTNAME_MAPPING = "OpenId_UserInfo_Lastname_Mapping";

    public static final String OPENID_REVOKE_ENDPOINT = "OpenId_Revoke_Endpoint";

    public static final String OPENID_ALLOW_INSECURE = "OpenId_Allow_insecure";

    @PluginParameter(name = OPENID_CLIENT_ID, label = "Client Id",
        description = "The client id registered for this Service Provider in order to authenticate requests")
    private String clientId;

    @PluginParameter(name = OPENID_CLIENT_SECRET, label = "Client Secret",
        description = "The client secret registered for this Service Provider in order to authenticate requests",
        sensitive = true, optional = true)
    private String clientSecret;

    @PluginParameter(name = OPENID_TOKEN_ENDPOINT, label = "\"token\" endpoint URL",
        description = "The Service Provider endpoint to authenticate and retrieve an Oauth2 token")
    @URL
    private String tokenEndpoint;

    @PluginParameter(name = OPENID_REDIRECT_URI, label = "Oauth2 redirect URI",
        description = "The redirect URI configured with the Oauth2 server")
    private String redirectUri;

    @PluginParameter(name = OPENID_USER_INFO_ENDPOINT, label = "\"user info\" endpoint URL",
        description = "The Service Provider endpoint to retrieve info about the authenticated user")
    @URL
    private String userInfoEndpoint;

    @PluginParameter(name = OPENID_USER_INFO_EMAIL_MAPPING, label = "Email mapping field",
        description = "The name of the field containing the user email in the Service Provider user info response")
    private String userInfoEmailMappingField;

    @PluginParameter(name = OPENID_USER_INFO_FIRSTNAME_MAPPING, label = "Firstname mapping field",
        description = "The name of the field containing the user firstname in the Service Provider user info response",
        optional = true)
    private String userInfoFirstnameMappingField;

    @PluginParameter(name = OPENID_USER_INFO_LASTNAME_MAPPING, label = "Lastname mapping field",
        description = "The name of the field containing the user lastname in the Service Provider user info response",
        optional = true)
    private String userInfoLastnameMappingField;

    @PluginParameter(name = OPENID_REVOKE_ENDPOINT, label = "\"revoke\" endpoint URL",
        description = "The Service Provider endpoint to deauthenticate", optional = true)
    @URL
    private String revokeEndpoint;

    private static final String BASIC_AUTH = "Basic ";

    private static final String BEARER_AUTH = "Bearer ";

    /**
     * Gson request and response converter
     */
    @Autowired
    private Gson gson;

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.login:#{null}}")
    private String proxyLogin;

    @Value("${http.proxy.password:#{null}}")
    private String proxyPassword;

    @Value("${http.proxy.port:#{null}}")
    private Integer proxyPort;

    @Value("${http.proxy.noproxy:#{T(java.util.Collections).emptyList()}}")
    private List<String> noProxy;

    @PluginParameter(name = OPENID_ALLOW_INSECURE, label = "Allow insecure SSL connection to openId server",
        description = "Only use this insecure configuration for tests purpose.", defaultValue = "false")
    private Boolean allowInsecure;

    private Feign feign;

    @PluginInit
    public void init() {
        feign = Feign.builder()
                     .client(new ApacheHttpClient(getHttpClient()))
                     .encoder(new FormEncoder())
                     .decoder(new ResponseEntityDecoder(new GsonDecoder(gson)))
                     .errorDecoder(new ClientErrorDecoder())
                     .decode404()
                     .contract(new FeignContractSupplier().get())
                     .build();
    }

    @Override
    public Class<OpenIdAuthenticationParams> getAuthenticationParamsType() {
        return OpenIdAuthenticationParams.class;
    }

    @Override
    public Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> authenticate(OpenIdAuthenticationParams params) {
        return token(params).flatMap(this::verify);
    }

    @Override
    public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
        //noinspection unchecked
        return jwtClaims.get(OPENID_CONNECT_TOKEN)
                        .map(o -> (String) o)
                        .toTry()
                        .mapFailure(Case($(instanceOf(NoSuchElementException.class)),
                                         ex -> new InternalAuthenticationServiceException(
                                             "Missing OpenID Connect token for deauthentication from service provider.")))
                        .flatMap(this::revoke);
    }

    @Override
    public Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> verify(String token) {
        return userInfo(token).map(userInfo -> {
            String email = userInfo.get(userInfoEmailMappingField);
            ServiceProviderAuthenticationInfo.UserInfo.Builder builder = new ServiceProviderAuthenticationInfo.UserInfo.Builder().withEmail(
                                                                                                                                     email)
                                                                                                                                 .withFirstname(
                                                                                                                                     userInfo.getOrDefault(
                                                                                                                                         userInfoFirstnameMappingField,
                                                                                                                                         email))
                                                                                                                                 .withLastname(
                                                                                                                                     userInfo.getOrDefault(
                                                                                                                                         userInfoLastnameMappingField,
                                                                                                                                         email));
            userInfo.forEach((key, value) -> {
                if (!key.equals(userInfoEmailMappingField) && !key.equals(userInfoFirstnameMappingField) && !key.equals(
                    userInfoLastnameMappingField)) {
                    builder.addMetadata(key, value);
                }
            });

            return new ServiceProviderAuthenticationInfo<>(builder.build(), new OpenIdConnectToken(token));
        });
    }

    private Try<String> token(OpenIdAuthenticationParams params) {
        String basicString = String.format("%s:%s", clientId, clientSecret);
        basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
        Map<String, String> headers = HashMap.of(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString);
        OpenIdConnectClient client = getOauth2Client(tokenEndpoint, headers);

        return Try.of(() -> new OpenIdTokenRequest(params.getCode(), redirectUri))
                  .map(client::token)
                  .transform(this::mapClientException)
                  .flatMap(response -> {
                      if (response.getStatusCode() != HttpStatus.OK) {
                          return Try.failure(new InternalAuthenticationServiceException(String.format(
                              "Service Provider rejected authentication request with status: %s",
                              response.getStatusCode())));
                      }
                      OpenIdTokenResponse body = response.getBody();
                      if (body == null) {
                          return Try.failure(new AuthenticationServiceException(
                              "Service Provider returned an empty response."));
                      }
                      if (!body.getTokenType().equalsIgnoreCase("bearer")) {
                          return Try.failure(new InsufficientAuthenticationException(String.format(
                              "Service Provider returned invalid token type \"%s\", expected \"bearer\".",
                              body.getTokenType())));
                      }
                      return Try.success(body);
                  })
                  .map(OpenIdTokenResponse::getAccessToken);
    }

    private Try<java.util.HashMap<String, String>> userInfo(String oauth2Token) {
        Map<String, String> headers = HashMap.of(HttpHeaders.AUTHORIZATION, BEARER_AUTH + oauth2Token);
        OpenIdConnectClient client = getOauth2Client(userInfoEndpoint, headers);

        return Try.of(client::userInfo).transform(this::mapClientException).flatMap(response -> {
            if (response.getStatusCode() != HttpStatus.OK) {
                return Try.failure(new InsufficientAuthenticationException(String.format(
                    "Service Provider rejected userInfo request with status: %s",
                    response.getStatusCode())));
            }
            java.util.HashMap<String, String> body = response.getBody();
            if (body == null) {
                return Try.failure(new AuthenticationServiceException("Service Provider returned an empty response."));
            }
            if (!body.containsKey(userInfoEmailMappingField)) {
                return Try.failure(new InsufficientAuthenticationException(String.format(
                    "Service Provider userInfo resopnse does not contain required field: %s",
                    userInfoEmailMappingField)));
            }
            return Try.success(body);
        });
    }

    private Try<Unit> revoke(String oauth2Token) {
        if (!Strings.isNullOrEmpty(revokeEndpoint)) {
            String basicString = String.format("%s:%s", clientId, Optional.ofNullable(clientSecret).orElse(""));
            basicString = Base64.getEncoder().encodeToString(basicString.getBytes());
            Map<String, String> headers = HashMap.of(HttpHeaders.AUTHORIZATION, BASIC_AUTH + basicString);

            OpenIdConnectClient client = getOauth2Client(revokeEndpoint, headers);

            // Execute side effect, and then nothing.
            // It's not like we're going to fail the user's logout operation and ask it to retry?
            // Just let the error fall into limbo.
            Try.run(() -> client.revoke(RevokeBody.build(oauth2Token))).onFailure(e -> LOGGER.error(e.getMessage(), e));
        }
        return Try.success(Unit.UNIT);
    }

    protected OpenIdConnectClient getOauth2Client(String url, Map<String, String> headers) {
        return feign.newInstance(new ExternalTarget<>(OpenIdConnectClient.class, url, headers.toJavaMap()));
    }

    private <T> Try<T> mapClientException(Try<T> call) {
        //noinspection unchecked
        return call.mapFailure(Case($(instanceOf(HttpClientErrorException.class)), ex -> {
            LOGGER.error(ex.getMessage(), ex);
            return new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }), Case($(instanceOf(HttpServerErrorException.class)), ex -> {
            LOGGER.error(ex.getMessage(), ex);
            return new AuthenticationServiceException(ex.getMessage(), ex);
        }), Case($(instanceOf(FeignException.class)), ex -> {
            LOGGER.error(ex.getMessage(), ex);
            return new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }));
    }

    /**
     * Copy/pasted from proxy-starter which is not enabled here because it is pernicious.
     */
    private HttpClient getHttpClient() {
        //  Kept in case there is some server that do not support being ask to create a TLSv1 connection while we can also speak in TLSv1.2...
        //  This allows use to force the usage of only TLSv1.2
        //  You just need to add a call to HttpClientBuilder#setSSLSocketFactor(sslsf) to activate it
        //        // specify some SSL parameter for clients only
        //        SSLContext sslcontext = SSLContexts.createDefault();
        //        // Allow TLSv1.2 protocol only
        //        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,
        //                                                                          new String[] { "TLSv1.2" },
        //                                                                          null,
        //                                                                          SSLConnectionSocketFactory
        //                                                                                  .getDefaultHostnameVerifier());
        HttpClientBuilder builder = HttpClients.custom();
        SSLContext unChecksslContext = null;
        SSLConnectionSocketFactory uncheckSslConnectionSocketFactory = null;
        Registry<ConnectionSocketFactory> sslUncheckedSocketFactoryRegistry = null;
        if (allowInsecure) {
            try {
                unChecksslContext = SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
                uncheckSslConnectionSocketFactory = new SSLConnectionSocketFactory(unChecksslContext,
                                                                                   NoopHostnameVerifier.INSTANCE);
                sslUncheckedSocketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                                                                   .register("https", uncheckSslConnectionSocketFactory)
                                                                   .build();
            } catch (Exception e) {
                LOGGER.error("Error creating SSL Context unchecked", e);
            }
        }
        PoolingHttpClientConnectionManager connManager;
        if (sslUncheckedSocketFactoryRegistry != null) {
            connManager = new PoolingHttpClientConnectionManager(sslUncheckedSocketFactoryRegistry);
        } else {
            connManager = new PoolingHttpClientConnectionManager();
        }
        connManager.setDefaultMaxPerRoute(10);
        connManager.setMaxTotal(20);
        builder.setConnectionManager(connManager).setKeepAliveStrategy((httpResponse, httpContext) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(httpResponse.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 5 * 1000;
        });

        if (unChecksslContext != null && uncheckSslConnectionSocketFactory != null) {
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            builder.setSSLContext(unChecksslContext);
            builder.setSSLSocketFactory(uncheckSslConnectionSocketFactory);
        }

        if ((proxyHost != null) && !proxyHost.isEmpty()) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            if (((proxyLogin != null) && !proxyLogin.isEmpty()) && ((proxyPassword != null)
                && !proxyPassword.isEmpty())) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()),
                                             new UsernamePasswordCredentials(proxyLogin, proxyPassword));
                builder.setDefaultCredentialsProvider(credsProvider);
            }
            if (noProxy != null) {
                HttpRoutePlanner routePlannerHandlingNoProxy = new DefaultProxyRoutePlanner(proxy) {

                    @Override
                    public HttpRoute determineRoute(final HttpHost host,
                                                    final HttpRequest request,
                                                    final HttpContext context) throws HttpException {
                        String hostname = host.getHostName();
                        if (noProxy.contains(hostname)) {
                            // Return direct route
                            return new HttpRoute(host);
                        }

                        return super.determineRoute(host, request, context);
                    }
                };
                return builder.setProxy(proxy).setRoutePlanner(routePlannerHandlingNoProxy).build();
            }
            return builder.setProxy(proxy).build();
        } else {
            return builder.build();
        }
    }
}
