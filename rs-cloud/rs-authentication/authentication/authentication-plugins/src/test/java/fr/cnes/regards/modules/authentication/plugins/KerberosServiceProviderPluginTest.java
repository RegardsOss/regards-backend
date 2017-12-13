/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.plugins;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.modules.authentication.plugins.impl.kerberos.KerberosSPParameters;
import fr.cnes.regards.modules.authentication.plugins.impl.kerberos.KerberosServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.plugins.impl.kerberos.Krb5TicketValidateAction;

/**
 *
 * Class KerberosServiceProviderPluginTest
 *
 * Test the Kerberos service provider plugin
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 *
 * @since 1.0-SNAPSHOT
 */
@Ignore
public class KerberosServiceProviderPluginTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(KerberosServiceProviderPluginTest.class);

    /**
     * Kerberos plugin to test
     */
    private static KerberosServiceProviderPlugin plugin;

    /**
     * REGARDS Principal to use for test
     */
    private static final String applicationPrincipal = "HTTP/po14173LX@REGARDS.CLOUD-ESPACE.SI.C-S.FR";

    /**
     * User login to connect for test
     */
    private static final String userPrincipal = "sbinda";

    /**
     *
     * Initialize LDAP Authentication plugin thought plugin utilities.
     *
     * @since 1.0-SNAPSHOT
     */
    @BeforeClass
    public static void init() {

        final String error = "The current system does not have kerberos command kinit to generate tgt. Test is skipped.";
        try {
            final String klistCommand = "klist";
            final Process process = Runtime.getRuntime().exec(klistCommand);
            if (process.waitFor() != 0) {
                LOG.warn(error);
                return;
            }
        } catch (final IOException | InterruptedException e1) {
            LOG.warn(error);
            return;
        }

        final URL urlkrb5 = ClassLoader.getSystemResource("krb5.conf");
        final URL keytabFilePath = ClassLoader.getSystemResource("regards.keytab");
        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(KerberosSPParameters.PRINCIPAL_PARAMETER, applicationPrincipal)
                .addParameter(KerberosSPParameters.REALM_PARAMETER, "REGARDS.CLOUD-ESPACE.SI.C-S.FR")
                .addParameter(KerberosSPParameters.LDAP_ADRESS_PARAMETER, "REGARDS-AD.CLOUD-ESPACE.SI.C-S.FR")
                .addParameter(KerberosSPParameters.LDAP_PORT_PARAMETER, "389")
                .addParameter(KerberosSPParameters.PARAM_LDAP_CN, "dc=REGARDS,dc=CLOUD-ESPACE,dc=SI,dc=C-S,dc=FR")
                .addParameter(KerberosSPParameters.KRB5_FILEPATH_PARAMETER, urlkrb5.getPath())
                .addParameter(KerberosSPParameters.KEYTAB_FILEPATH_PARAMETER, keytabFilePath.getPath()).getParameters();
        try {
            // instantiate plugin
            plugin = PluginUtils.getPlugin(parameters, KerberosServiceProviderPlugin.class, Arrays
                    .asList("fr.cnes.regards.cloud.gateway.authentication.plugins.impl.kerberos"), new HashMap<>());
            Assert.assertNotNull(plugin);
        } catch (final PluginUtilsRuntimeException e) {
            Assert.fail();
        }
    }

    /**
     *
     * Check authentication to REGARDS system with a kerberos ticket
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_SEC_120")
    @Requirement("REGARDS_DSL_ADM_PLG_210")
    @Purpose("Check authentication to REGARDS system with a kerberos ticket")
    public void checkKerberosTicketValidation() {
        if (plugin != null) {
            final byte[] ticket = generateKerberosTicket(applicationPrincipal, userPrincipal);
            final ExternalAuthenticationInformations authInformations = new ExternalAuthenticationInformations(
                    userPrincipal, "test", ticket, "");
            Assert.assertTrue(plugin.checkTicketValidity(authInformations));
            final UserDetails details = plugin.getUserInformations(authInformations);
            Assert.assertTrue((details.getName() != null) && !details.getName().isEmpty());
            LOG.info("Email retrieved : {}", details.getName());
        } else {
            LOG.warn("Skip Test");
        }
    }

    /**
     *
     * Authenticate to Kerberos server to generate a valid ticket
     *
     * @param pApplicationPrincipal
     *            Kerberos princiapl
     * @param pUserPrincipal
     *            user to connect
     * @return valid ticket
     * @since 1.0-SNAPSHOT
     */
    public byte[] generateKerberosTicket(final String pApplicationPrincipal, final String pUserPrincipal) {
        try {

            final Oid krb5Oid = new Oid(Krb5TicketValidateAction.KERB_V5_OID);

            final GSSManager manager = GSSManager.getInstance();

            // Authenticate test user
            final GSSName clientName = manager.createName(pUserPrincipal, GSSName.NT_USER_NAME);
            final GSSCredential clientCred = manager.createCredential(clientName, 8 * 3600, krb5Oid,
                                                                      GSSCredential.INITIATE_ONLY);

            // Authenticate Regards application
            final GSSName serverName = manager.createName(pApplicationPrincipal, GSSName.NT_USER_NAME);
            final GSSContext context = manager.createContext(serverName, krb5Oid, clientCred,
                                                             GSSContext.DEFAULT_LIFETIME);
            context.requestMutualAuth(false);
            context.requestInteg(false);
            context.requestCredDeleg(true);

            byte[] token = new byte[0];
            token = context.initSecContext(token, 0, token.length);

            System.out.println(context.getSrcName());
            System.out.println(context.getTargName());
            System.out.println("Is established : " + context.isEstablished());
            System.out.println("Cred. delegation : " + context.getCredDelegState());
            System.out.println("Lifetime : " + context.getLifetime());

            context.dispose();

            return token;

        } catch (final GSSException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

}
