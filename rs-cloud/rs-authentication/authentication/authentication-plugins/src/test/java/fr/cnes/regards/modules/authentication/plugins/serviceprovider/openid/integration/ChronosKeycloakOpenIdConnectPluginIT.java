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
package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.integration;

import com.google.gson.Gson;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdAuthenticationParams;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdConnectPlugin;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdConnectToken;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
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
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_EMAIL_MAPPING, "sub"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_FIRSTNAME_MAPPING, "given_name"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_LASTNAME_MAPPING, "family_name"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT, (String) null)
            );
        PluginConfiguration conf = PluginConfiguration.build(OpenIdConnectPlugin.class, "", parameters);
        OpenIdConnectPlugin plugin = PluginUtils.getPlugin(conf, new HashMap<>());

        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> result =
            plugin.authenticate(
                new OpenIdAuthenticationParams(
                    "281e2396-455f-4158-9a27-4c5348f19e4d.0d070daa-c132-4b76-b099-74c09ab5ea34.fd522e1f-281d-41c9-b0ad-911ea074365a"
                )
            );

        assertThat(result.isSuccess()).isTrue();
        Map<String, String> authenticationInfo = result.get().getAuthenticationInfo();
        assertThat(authenticationInfo.containsKey(OpenIdConnectPlugin.OPENID_CONNECT_TOKEN)).isTrue();
        ServiceProviderAuthenticationInfo.UserInfo userInfo = result.get().getUserInfo();
        assertThat(userInfo.getEmail()).isEqualTo("a6d83cd3-e458-44a6-9022-5da8fb87c8f1");
        assertThat(userInfo.getFirstname()).isEqualTo("User Validation");
        assertThat(userInfo.getLastname()).isEqualTo("Validation");
        assertThat(userInfo.getMetadata().get("email_verified")).isEqualTo(Option.some(Boolean.toString(false)));
        assertThat(userInfo.getMetadata().get("preferred_username")).isEqualTo(Option.some("uvalidation"));
    }

}