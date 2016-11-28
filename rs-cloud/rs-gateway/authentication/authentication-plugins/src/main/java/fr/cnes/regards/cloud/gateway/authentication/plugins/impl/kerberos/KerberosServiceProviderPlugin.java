/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl.kerberos;

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

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;

/**
 *
 * Class KerberosServiceProviderPlugin
 *
 * Kerberos Server Provider Plugin.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Plugin(author = "CS-SI", description = "Kerberos Service Provider", id = "KerberosServiceProviderPlugin",
        version = "1.0")
public class KerberosServiceProviderPlugin implements IServiceProviderPlugin {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(KerberosServiceProviderPlugin.class);

    /**
     * REGARDS ticket service
     */
    @PluginParameter(name = KerberosSPParameters.PRINCIPAL_PARAMETER, description = "REGARDS ticket service")
    private String principal;

    /**
     * Kerberos domain name
     */
    @PluginParameter(name = KerberosSPParameters.REALM_PARAMETER, description = "Kerberos domain name")
    private String realm;

    /**
     * Adress to connect to Active Directory LDAP
     */
    @PluginParameter(name = KerberosSPParameters.LDAP_ADRESS_PARAMETER,
            description = "Adress to connect to Active Directory LDAP")
    private String ldapAdress;

    /**
     * Port to connect to Active Directory LDAP
     */
    @PluginParameter(name = KerberosSPParameters.LDAP_PORT_PARAMETER,
            description = "Port to connect to Active Directory LDAP (default=389)")
    private String ldapPort;

    /**
     * LDAP Root CN
     */
    @PluginParameter(name = KerberosSPParameters.PARAM_LDAP_CN, description = "LDAP Root CN")
    private String ldapCN;

    /**
     * LDAP Filter to find the User object
     */
    @PluginParameter(name = KerberosSPParameters.PARAM_LDAP_USER_FILTER_ATTTRIBUTE,
            description = "LDAP Filter to find the user object (default = (ObjectClass=user))")
    private String ldapSearchUserFilter;

    /**
     * LDAP User login attribute.
     */
    @PluginParameter(name = KerberosSPParameters.PARAM_LDAP_USER_LOGIN_ATTTRIBUTE,
            description = "LDAP User parameter containing user login (default=sAMAccountName)")
    private String ldapUserLoginAttribute;

    /**
     * LDAP parameter for user email (default=mail)
     */
    @PluginParameter(name = KerberosSPParameters.PARAM_LDAP_EMAIL_ATTTRIBUTE,
            description = "LDAP parameter for user email (default=mail)")
    private String ldapEmailAttribute;

    /**
     * Kerberos configuration file (krb5.conf)
     */
    @PluginParameter(name = KerberosSPParameters.KRB5_FILEPATH_PARAMETER,
            description = "Kerberos configuration file (krb5.conf)")
    private String krb5FilePath;

    /**
     * Kerberos Keytab file
     */
    @PluginParameter(name = KerberosSPParameters.KEYTAB_FILEPATH_PARAMETER, description = "Kerberos Keytab file")
    private String keytabFilePath;

    /**
     * Current ValidationAction. Used to share kerberos context between ticket validation and retrieve user
     * informtaions.
     */
    private Krb5TicketValidateAction validateAction;

    /**
     *
     * Initialize default values if not parameters not set. Initialize system property for kerberos management.
     *
     * @since 1.0-SNAPSHOT
     */
    @PluginInit
    public void pluginInitialization() {

        // Initialize default values if not parameters not set
        if (ldapPort == null) {
            ldapPort = "389";
        }
        if (ldapSearchUserFilter == null) {
            ldapSearchUserFilter = "(ObjectClass=user)";
        }
        if (ldapUserLoginAttribute == null) {
            ldapUserLoginAttribute = "sAMAccountName";
        }
        if (ldapEmailAttribute == null) {
            ldapEmailAttribute = "mail";
        }

        // Initialize system property for kerberos management
        System.setProperty("java.security.krb5.conf", krb5FilePath);
        System.setProperty("javax.security.auth.useSubjectCredsOnly", Boolean.FALSE.toString());
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

        // Get credential
        final UserDetails userDetails = new UserDetails();
        userDetails.setName(pAuthInformations.getUserName());
        userDetails.setTenant(pAuthInformations.getProject());
        if ((validateAction != null) && (validateAction.getGssContext() != null)) {
            try {
                final GSSCredential credentialDeleg = validateAction.getGssContext().getDelegCred();
                @SuppressWarnings("restriction")
                final Subject subject = com.sun.security.jgss.GSSUtil.createSubject(credentialDeleg.getName(),
                                                                                    credentialDeleg); // NOSONAR
                // LDAP : connection.
                final KerberosLdapAction ldapAction = new KerberosLdapAction(ldapAdress, Integer.valueOf(ldapPort),
                        pAuthInformations.getUserName());
                Subject.doAs(subject, ldapAction);
                final String mail = ldapAction.getUserEmail(ldapCN, ldapEmailAttribute, ldapSearchUserFilter,
                                                            ldapUserLoginAttribute);
                userDetails.setEmail(mail);
            } catch (final GSSException | LdapException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        if (validateAction != null) {
            validateAction.closeContext();
        }

        return userDetails;
    }

    /**
     *
     * Retrieve Jaas configuration to decode ticket
     *
     * @param pPrincipal
     *            Principal for Kerberos server authentication
     * @param pRealm
     *            Realm of the kerberos server
     * @param pKeyTab
     *            Kerberos keytab file. Supplied by the Kerberos server administrator
     * @return {@link Configuration}
     * @since 1.0-SNAPSHOT
     */
    public static Configuration getJaasConf(final String pPrincipal, final String pRealm, final File pKeyTab) {

        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(final String pName) {
                final Map<String, String> options = new HashMap<>();
                options.put("principal", pPrincipal);
                options.put("realm", pRealm);
                options.put("keyTab", pKeyTab.getAbsolutePath());
                options.put("refreshKrb5Config", Boolean.TRUE.toString());
                options.put("doNotPrompt", Boolean.TRUE.toString());
                options.put("useKeyTab", Boolean.TRUE.toString());
                options.put("storeKey", Boolean.TRUE.toString());
                options.put("isInitiator", Boolean.FALSE.toString());
                options.put("debug", Boolean.FALSE.toString());
                return new AppConfigurationEntry[] { new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule", LoginModuleControlFlag.REQUIRED, options) };
            }
        };
    }

    /**
     *
     * Decode a given kerberos ticket.
     *
     * @param pPrincipal
     *            Kerberos principal
     * @param pTicket
     *            Kerberos ticket to decode
     * @param pConfig
     *            Jaas Configuraion
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    public boolean decode(final String pPrincipal, final byte[] pTicket, final Configuration pConfig) {

        LoginContext ctx = null;
        try {
            // define the principal who will validate the ticket
            final Principal krbPrincipal = new KerberosPrincipal(pPrincipal, KerberosPrincipal.KRB_NT_UNKNOWN);
            final Set<Principal> principals = new HashSet<>();
            principals.add(krbPrincipal);

            // define the subject to execute our secure action as
            final Subject subject = new Subject(false, principals, new HashSet<Object>(), new HashSet<Object>());

            // login the subject
            ctx = new LoginContext("", subject, null, pConfig);
            ctx.login();

            // create a validator for the ticket and execute it
            validateAction = new Krb5TicketValidateAction(pTicket, pPrincipal);
            Subject.doAs(subject, validateAction);
            return true;
        } catch (final PrivilegedActionException e) {
            LOG.error("Invalid ticket for {} : {}", pPrincipal, e.getMessage(), e);
        } catch (final LoginException e) {
            LOG.error("Error creating validation LoginContext for {} : {}", pPrincipal, e.getMessage(), e);
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
