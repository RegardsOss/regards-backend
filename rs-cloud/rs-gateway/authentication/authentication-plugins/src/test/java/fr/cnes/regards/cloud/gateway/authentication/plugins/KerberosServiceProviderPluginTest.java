/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import java.net.URL;
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

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.cloud.gateway.authentication.plugins.impl.KerberosServiceProviderPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.impl.Krb5TicketValidateAction;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Class KerberosServiceProviderPluginTest
 *
 * Test the Kerberos service provider plugin
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
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
    private final static String principal = "HTTP/po14173LX@REGARDS.CLOUD-ESPACE.SI.C-S.FR";

    /**
     *
     * Initialize LDAP Authentication plugin thought plugin utilities.
     *
     * @since 1.0-SNAPSHOT
     */
    @BeforeClass
    public static void init() {

        final URL url = ClassLoader.getSystemResource("regards.keytab");
        final URL urlkrb5 = ClassLoader.getSystemResource("krb5.conf");

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(KerberosServiceProviderPlugin.PRINCIPAL_PARAMETER, principal)
                .addParameter(KerberosServiceProviderPlugin.REALM_PARAMETER, "REGARDS.CLOUD-ESPACE.SI.C-S.FR")
                .addParameter(KerberosServiceProviderPlugin.KRB5_FILEPATH_PARAMETER, urlkrb5.getPath())
                .addParameter(KerberosServiceProviderPlugin.KEYTAB_FILEPATH_PARAMETER, url.getPath()).getParameters();
        try {
            // instantiate plugin
            plugin = PluginUtils.getPlugin(parameters, KerberosServiceProviderPlugin.class);
            Assert.assertNotNull(plugin);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }

    }

    public void testContext() {
        // Nothing to do. Only check plugin initialization
    }

    @Test
    @Ignore
    public void checkKerberosTicketValidation() {
        final byte[] ticket = generateKerberosTicket(principal);
        final ExternalAuthenticationInformations authInformations = new ExternalAuthenticationInformations("test",
                "test", ticket, "");
        Assert.assertTrue(plugin.checkTicketValidity(authInformations));
    }

    /**
     *
     * Authenticate to Kerberos server to generate a valid ticket
     *
     * @param principal
     *            Kerberos princiapl
     * @return valid ticket
     * @since 1.0-SNAPSHOT
     */
    public byte[] generateKerberosTicket(final String principal) {
        try {

            final Oid krb5Oid = new Oid(Krb5TicketValidateAction.KERB_V5_OID);

            final GSSManager manager = GSSManager.getInstance();

            // Authenticate test user
            final GSSName clientName = manager.createName("sbinda", GSSName.NT_USER_NAME);
            final GSSCredential clientCred = manager.createCredential(clientName, 8 * 3600, krb5Oid,
                                                                      GSSCredential.INITIATE_ONLY);

            // Authenticate Regards application
            final GSSName serverName = manager.createName(principal, GSSName.NT_USER_NAME);
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
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

}
