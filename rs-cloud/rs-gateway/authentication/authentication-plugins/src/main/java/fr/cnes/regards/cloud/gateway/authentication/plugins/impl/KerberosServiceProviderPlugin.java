/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl;

import java.io.File;
import java.net.URL;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;

@Plugin(author = "CS-SI", description = "Kerberos Service Provider", id = "KerberosServiceProviderPlugin",
        version = "1.0")
public class KerberosServiceProviderPlugin implements IServiceProviderPlugin {

    public static final String PRINCIPAL_PARAMETER = "principal";

    public static final String REALM_PARAMETER = "realm";

    public static final String KRB5_FILEPATH_PARAMETER = "krb5FilePath";

    public static final String KEYTAB_FILEPATH_PARAMETER = "keytabFilePath";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(KerberosServiceProviderPlugin.class);

    @PluginParameter(name = PRINCIPAL_PARAMETER, description = "REGARDS ticket service")
    private String principal;

    @PluginParameter(name = REALM_PARAMETER, description = "Kerberos domain name")
    private String realm;

    @PluginParameter(name = KRB5_FILEPATH_PARAMETER, description = "Kerberos configuration file (krb5.conf)")
    private String krb5FilePath;

    @PluginParameter(name = KEYTAB_FILEPATH_PARAMETER, description = "Kerberos Keytab file")
    private String keytabFilePath;

    @PluginInit
    public void initJavaProperties() {
        System.setProperty("java.security.krb5.conf", krb5FilePath);
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        final URL url = ClassLoader.getSystemResource("jaas2.conf");
        System.setProperty("java.security.auth.login.config", url.toString());
    }

    @Override
    public boolean checkTicketValidity(final ExternalAuthenticationInformations pAuthInformations) {
        return decode(principal, pAuthInformations.getTicket(),
                      getJaasConf(principal, realm, new File(keytabFilePath)));
    }

    @Override
    public UserDetails getUserInformations(final ExternalAuthenticationInformations pAuthInformations) {
        // TODO : Get informations from LDAP. User LdapAuthenticationPlugin ?
        final UserDetails userDetails = new UserDetails();
        userDetails.setEmail(pAuthInformations.getUserName());
        userDetails.setName(pAuthInformations.getUserName());
        userDetails.setTenant(pAuthInformations.getProject());
        return userDetails;
    }

    /**
     *
     * Retrieve Jaas configuration to decode ticket
     *
     * @param principal
     * @param realm
     *            Realm of the kerberos server
     * @param keyTab
     *            Kerberos keytab file. Supplied by the Kerberos server administrator
     * @return
     * @since 1.0-SNAPSHOT
     */
    public static Configuration getJaasConf(final String principal, final String realm, final File keyTab) {

        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
                final Map<String, String> options = new HashMap<>();
                options.put("principal", principal);
                options.put("realm", realm);
                options.put("keyTab", keyTab.getAbsolutePath());
                options.put("refreshKrb5Config", "true");
                options.put("doNotPrompt", "true");
                options.put("useKeyTab", "true");
                options.put("storeKey", "true");
                options.put("isInitiator", "false");
                options.put("debug", "true");
                return new AppConfigurationEntry[] { new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule", LoginModuleControlFlag.REQUIRED, options) };
            }
        };
    }

    public boolean decode(final String spn, final byte[] ticket, final Configuration config) {
        LoginContext ctx = null;
        try {
            // define the principal who will validate the ticket
            final Principal krbPrincipal = new KerberosPrincipal(spn, KerberosPrincipal.KRB_NT_UNKNOWN);
            final Set<Principal> principals = new HashSet<>();
            principals.add(krbPrincipal);

            // define the subject to execute our secure action as
            final Subject subject = new Subject(false, principals, new HashSet<Object>(), new HashSet<Object>());

            // login the subject
            ctx = new LoginContext("", subject, null, config);
            ctx.login();

            // create a validator for the ticket and execute it
            final Krb5TicketValidateAction validateAction = new Krb5TicketValidateAction(ticket, spn);
            Subject.doAs(subject, validateAction);
            return true;
        } catch (final PrivilegedActionException e) {
            LOG.error("Invalid ticket for {} : {}", spn, e.getMessage(), e);
        } catch (final LoginException e) {
            LOG.error("Error creating validation LoginContext for {} : {}", spn, e.getMessage(), e);
        } finally {
            try {
                if (ctx != null) {
                    ctx.logout();
                }
            } catch (final LoginException e) { /* noop */
                LOG.error(e.getMessage(), e);
            }
        }
        return false;
    }

}
