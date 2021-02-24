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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.request.OpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdUserInfoResponse;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=chronos_keycloak_authentication_service_provider_tests",
    })
public class ChronosKeycloakOpenIdConnectPluginIT extends AbstractRegardsServiceIT {

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        PluginUtils.setup();
    }

    @Test
    // 1. Go to https://chronos-valid-dev.cloud-espace.si.c-s.fr:8443/auth/realms/chronos/protocol/openid-connect/auth?client_id=regards&redirect_uri=http://plop.com&response_mode=fragment&response_type=code&scope=openid
    // 2. Enter login / password : uvalidation/password
    // 3. After redirect, get query param "code" in URL
    // 4 Copy paste in OpenIdAuthenticationParams below
    // 5. Run test, quickly before the token is invalidated.
    // NB: After each failure, a new code is required.
    @Ignore("Uncomment when testing manually.")
    public void chronos_keycloak_auth() throws EncryptionException, NotAvailablePluginConfigurationException {
        // Set all parameters
        Set<IPluginParam> parameters = IPluginParam
            .set(
                IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_ID, "regards"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_SECRET, encryptionService.encrypt("")),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_TOKEN_ENDPOINT, "https://chronos-valid-dev.cloud-espace.si.c-s.fr:8443/auth/realms/chronos/protocol/openid-connect/token"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_ENDPOINT, "https://chronos-valid-dev.cloud-espace.si.c-s.fr:8443/auth/realms/chronos/protocol/openid-connect/userinfo"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT, (String) null)
            );
        PluginConfiguration conf = PluginConfiguration.build(ChronosKeycloakOpenIdConnectPlugin.class, "", parameters);
        ChronosKeycloakOpenIdConnectPlugin plugin = PluginUtils.getPlugin(conf, new HashMap<>());

        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> result =
            plugin.authenticate(
                new OpenIdAuthenticationParams(
                    "40f00227-d934-4a45-ac8d-0757f1776737.c92cb1d0-8505-4db0-91ee-a93ad404f0a3.fd522e1f-281d-41c9-b0ad-911ea074365a",
                    "http://plop.com"
                )
            );

        assertThat(result.isSuccess()).isTrue();
        Map<String, String> authenticationInfo = result.get().getAuthenticationInfo();
        assertThat(authenticationInfo.containsKey(OpenIdConnectPlugin.OPENID_CONNECT_TOKEN)).isTrue();
        assertThat(authenticationInfo.containsKey(OpenIdConnectPlugin.OPENID_CONNECT_PLUGIN)).isTrue();
        ServiceProviderAuthenticationInfo.UserInfo userInfo = result.get().getUserInfo();
        assertThat(userInfo.getEmail()).isEqualTo("User Validation Validation");
        assertThat(userInfo.getFirstname()).isEqualTo("User Validation");
        assertThat(userInfo.getLastname()).isEqualTo("Validation");
        assertThat(userInfo.getMetadata().get("sub")).isEqualTo(Option.some("a6d83cd3-e458-44a6-9022-5da8fb87c8f1"));
        assertThat(userInfo.getMetadata().get("emailVerified")).isEqualTo(Option.some(Boolean.toString(false)));
        assertThat(userInfo.getMetadata().get("preferredUsername")).isEqualTo(Option.some("uvalidation"));
    }

}

@RestClient(name = "chronos-keycloak-open-id-connect", contextId = "chronos-keycloak-open-id-connect")
@RequestMapping(
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
interface ChronosKeycloakOpenIdConnectClient extends IOpenIdConnectClient<ChronosKeycloakOpenIdUserInfoResponse> {

    @PostMapping
    @ResponseBody
    ResponseEntity<OpenIdTokenResponse> token(@RequestBody OpenIdTokenRequest request);

    @GetMapping
    @ResponseBody
    ResponseEntity<ChronosKeycloakOpenIdUserInfoResponse> userInfo();

    @PostMapping
    ResponseEntity<Void> revoke(@RequestParam("token") String token);
}

class ChronosKeycloakOpenIdUserInfoResponse extends OpenIdUserInfoResponse {
    @SerializedName("name")
    private final String name;

    @SerializedName("sub")
    private final String sub;

    @SerializedName("email_verified")
    private final boolean emailVerified;

    @SerializedName("preferred_username")
    private final String preferredUsername;

    @SerializedName("given_name")
    private final String givenName;

    @SerializedName("family_name")
    private final String familyName;

    public ChronosKeycloakOpenIdUserInfoResponse(
        String name,
        String sub,
        boolean emailVerified,
        String preferredUsername,
        String givenName,
        String familyName
    ) {
        this.name = name;
        this.sub = sub;
        this.emailVerified = emailVerified;
        this.preferredUsername = preferredUsername;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    public String getName() {
        return name;
    }

    public String getSub() {
        return sub;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChronosKeycloakOpenIdUserInfoResponse that = (ChronosKeycloakOpenIdUserInfoResponse) o;
        return emailVerified == that.emailVerified
            && Objects.equals(name, that.name)
            && Objects.equals(sub, that.sub)
            && Objects.equals(preferredUsername, that.preferredUsername)
            && Objects.equals(givenName, that.givenName)
            && Objects.equals(familyName, that.familyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sub, emailVerified, preferredUsername, givenName, familyName);
    }

        @Override
    public ServiceProviderAuthenticationInfo.UserInfo toDomain() {
        return new ServiceProviderAuthenticationInfo.UserInfo.Builder()
            .withEmail(name)
            .withFirstname(givenName)
            .withLastname(familyName)
            .addMetadata("sub", sub)
            .addMetadata("emailVerified", Boolean.toString(emailVerified))
            .addMetadata("preferredUsername", preferredUsername)
            .build();
    }
}
