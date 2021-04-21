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
import fr.cnes.regards.framework.modules.plugins.domain.parameter.StringPluginParam;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=theia_authentication_service_provider_tests",
    })
public class TheiaOpenIdConnectPluginIT extends AbstractRegardsServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(TheiaOpenIdConnectPluginIT.class);

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        PluginUtils.setup();
    }

    @Test
    // 1. Go to https://sso.theia-land.fr/oauth2/authorize?redirect_uri=http://vm-perf.cloud-espace.si.c-s.fr/authenticate/perf/theia&response_type=code&scope=openid&client_id=rRLGfEh6jtXjiiGUf53UOdmJLXga
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
                IPluginParam.build(OpenIdConnectPlugin.OPENID_REDIRECT_URI, "http://vm-perf.cloud-espace.si.c-s.fr/authenticate/perf/theia"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_TOKEN_ENDPOINT, "https://sso.theia-land.fr/oauth2/token"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_ENDPOINT, "https://sso.theia-land.fr/theia/services/userinfo"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_EMAIL_MAPPING, "http://theia.org/claims/emailaddress"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_FIRSTNAME_MAPPING, "http://theia.org/claims/givenname"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_LASTNAME_MAPPING, "http://theia.org/claims/lastname"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT, (String) null)
            );
        PluginConfiguration conf = PluginConfiguration.build(OpenIdConnectPlugin.class, "", parameters);
        OpenIdConnectPlugin plugin = PluginUtils.getPlugin(conf, new HashMap<>());

        Try<ServiceProviderAuthenticationInfo<OpenIdConnectToken>> result =
            plugin.authenticate(
                new OpenIdAuthenticationParams(
                    "f4136307-1223-353c-9fdc-87bfe5c10be4"
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
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/organization")).isEqualTo(Option.some("CS Group"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/function")).isEqualTo(Option.some("ff"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/type")).isEqualTo(Option.some("person"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/streetaddress")).isEqualTo(Option.some("ff"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/source")).isEqualTo(Option.some("theia"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/country")).isEqualTo(Option.some("FR"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/ignkey")).isEqualTo(Option.some(""));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/ignauthentication")).isEqualTo(Option.some(""));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/foreignauthorization")).isEqualTo(Option.some(Boolean.toString(false)));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/role")).isEqualTo(Option.some("Internal/identity,Internal/everyone"));
        assertThat(userInfo.getMetadata().get("http://theia.org/claims/regDate")).isEqualTo(Option.some("1614872166974"));
    }

}

