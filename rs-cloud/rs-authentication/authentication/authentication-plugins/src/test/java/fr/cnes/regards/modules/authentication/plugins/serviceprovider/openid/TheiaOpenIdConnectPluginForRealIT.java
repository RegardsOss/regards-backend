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
import fr.cnes.regards.framework.modules.plugins.domain.parameter.StringPluginParam;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.request.OpenIdTokenRequest;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdTokenResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.response.OpenIdUserInfoResponse;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.TheiaOpenIdConnectPlugin;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        "spring.jpa.properties.hibernate.default_schema=theia_for_real_authentication_service_provider_tests",
    })
public class TheiaOpenIdConnectPluginForRealIT extends AbstractRegardsServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(TheiaOpenIdConnectPluginForRealIT.class);

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        PluginUtils.setup();
    }

    @Test
    // 1. Go to https://sso.theia-land.fr/oauth2/authorize?redirect_uri=http://vm-perf.cloud-espace.si.c-s.fr/auth/perf&response_type=code&scope=openid&client_id=rRLGfEh6jtXjiiGUf53UOdmJLXga
    // 2. Enter your login / password
    // 3. After redirect, get query param "code" in URL
    // 4. Copy paste in OpenIdAuthenticationParams below
    // 5. Paste client secret into secretStr variable
    // 6. Run test, quickly before the token is invalidated.
    // NB: After each failure/success, a new code is required.
    @Ignore("Uncomment when testing manually.")
    public void theia_auth() throws EncryptionException, NotAvailablePluginConfigurationException {

        System.setProperty("http.proxyHost", "proxy2.si.c-s.fr");
        System.setProperty("http.proxyPort", "3128");
        System.setProperty("https.proxyHost", "proxy2.si.c-s.fr");
        System.setProperty("https.proxyPort", "3128");

        // Set all parameters
        String secretStr = "";
        StringPluginParam secret = IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_SECRET, encryptionService.encrypt(secretStr));
        secret.setDecryptedValue(secretStr);
        Set<IPluginParam> parameters = IPluginParam
            .set(
                IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_ID, "rRLGfEh6jtXjiiGUf53UOdmJLXga"),
                secret,
                IPluginParam.build(OpenIdConnectPlugin.OPENID_TOKEN_ENDPOINT, "https://sso.theia-land.fr/oauth2/token"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_ENDPOINT, "https://sso.theia-land.fr/theia/services/userinfo"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT, (String) null)
            );
        PluginConfiguration conf = PluginConfiguration.build(TheiaOpenIdConnectPlugin.class, "", parameters);
        TheiaOpenIdConnectPlugin plugin = PluginUtils.getPlugin(conf, new HashMap<>());

        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> result =
            plugin.authenticate(
                new OpenIdAuthenticationParams(
                    "54a5511b-3305-3433-ba0b-db815d494155",
                    "http://vm-perf.cloud-espace.si.c-s.fr/auth/perf"
                )
            );

        if (result.isFailure()) {
            LOG.debug("import of model failed", result.getCause());
            Assert.fail();
        }
        Map<String, String> authenticationInfo = result.get().getAuthenticationInfo();
        assertThat(authenticationInfo.containsKey(OpenIdConnectPlugin.OPENID_CONNECT_TOKEN)).isTrue();
        ServiceProviderAuthenticationInfo.UserInfo userInfo = result.get().getUserInfo();
        assertThat(userInfo.getEmail()).isEqualTo("arnaud@monkeypatch.io");
        assertThat(userInfo.getFirstname()).isEqualTo("Arnaud");
        assertThat(userInfo.getLastname()).isEqualTo("Bos");
        assertThat(userInfo.getMetadata().get("organization")).isEqualTo(Option.some("CS Group"));
        assertThat(userInfo.getMetadata().get("function")).isEqualTo(Option.some("ff"));
        assertThat(userInfo.getMetadata().get("type")).isEqualTo(Option.some("person"));
        assertThat(userInfo.getMetadata().get("streetAddress")).isEqualTo(Option.some("ff"));
        assertThat(userInfo.getMetadata().get("source")).isEqualTo(Option.some("theia"));
        assertThat(userInfo.getMetadata().get("country")).isEqualTo(Option.some("FR"));
        assertThat(userInfo.getMetadata().get("ignKey")).isEqualTo(Option.some(""));
        assertThat(userInfo.getMetadata().get("ignAuthentication")).isEqualTo(Option.some(""));
        assertThat(userInfo.getMetadata().get("role")).isEqualTo(Option.some("Internal/identity,Internal/everyone"));
        assertThat(userInfo.getMetadata().get("regDate")).isEqualTo(Option.some("1614872166974"));
    }

}

