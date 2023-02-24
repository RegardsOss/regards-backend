/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 10/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.modules.authentication.plugins.identityprovider.ldap;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.domain.plugin.IAuthenticationPlugin;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class LdapAuthenticationPlugin
 * <p>
 * LDAP Authentication plugin.
 *
 * @author Sébastien Binda
 * @since 1.0
 */
@Plugin(author = "CS-SI",
        description = "LDAP authentication plugin",
        id = "LdapAuthenticationPlugin",
        version = "1.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "www.cnes.fr")
public class LdapAuthenticationPlugin implements IAuthenticationPlugin {

    /**
     * LDAP host label
     */
    public static final String PARAM_LDAP_HOST = "ldapHost";

    /**
     * LDAP host port
     */
    public static final String PARAM_LDAP_PORT = "ldapPort";

    /**
     * LDAP CN
     */
    public static final String PARAM_LDAP_CN = "ldapCN";

    /**
     * LDAP Search filter parameter label
     */
    public static final String PARAM_LDAP_USER_FILTER_ATTTRIBUTE = "ldapSearchUserFilter";

    /**
     * LDAP Email attribute to retrieve
     */
    public static final String PARAM_LDAP_USER_LOGIN_ATTTRIBUTE = "ldapUserLoginAttribute";

    /**
     * LDAP Email attribute to retrieve
     */
    public static final String PARAM_LDAP_USER_EMAIL_ATTTRIBUTE = "ldapEmailAttribute";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAuthenticationPlugin.class);

    /**
     * LDAP Host access
     */
    @PluginParameter(name = PARAM_LDAP_HOST, label = "LDAP Server address")
    private String ldapHost;

    /**
     * LDAP Port
     */
    @PluginParameter(name = PARAM_LDAP_PORT, label = "LDAP Server port", defaultValue = "389", optional = true)
    private String ldapPort;

    /**
     * LDAP DN
     */
    @PluginParameter(name = PARAM_LDAP_CN,
                     label = "LDAP Bind DN",
                     description = "Value exemple : ou=people,ou=commun,o=company")
    private String ldapDN;

    /**
     * LDAP User login attribute.
     */
    @PluginParameter(name = PARAM_LDAP_USER_LOGIN_ATTTRIBUTE,
                     label = "LDAP UID",
                     description = "LDAP User parameter containing user login. Default value is 'sAMAccountName'.",
                     optional = true,
                     defaultValue = "sAMAccountName")
    private String ldapUserLoginAttribute;

    /**
     * LDAP Filter to find the User object
     */
    @PluginParameter(name = PARAM_LDAP_USER_FILTER_ATTTRIBUTE,
                     label = "LDAP Filter",
                     description = "LDAP Filter to find the user object. Default value is '(ObjectClass=person)'.",
                     optional = true,
                     defaultValue = "(ObjectClass=person)")
    private String ldapSearchUserFilter;

    /**
     * LDAP email attribute label
     */
    @PluginParameter(name = PARAM_LDAP_USER_EMAIL_ATTTRIBUTE,
                     label = "LDAP email attribute",
                     description = "LDAP parameter for user email. Default value is 'mail'.",
                     optional = true,
                     defaultValue = "mail")
    private String ldapEmailAttribute;

    @Override
    public AuthenticationPluginResponse authenticate(final String pLogin, final String pPassword, final String pScope) {

        Boolean accessGranted = false;
        String errorMessage = null;
        String email = "";
        // Create LDAP Connection
        final LdapConnection connection = getLdapConnection(ldapHost, Integer.parseInt(ldapPort));

        // Create DN parameter with login in uid parameter.
        String dn = "uid=" + pLogin;
        if ((ldapDN != null) && !ldapDN.isEmpty()) {
            dn = dn + "," + ldapDN;
        }

        try {
            // Check connection
            if (!connection.connect()) {
                errorMessage = "Connection failed to LDAP Server";
            } else {
                connection.bind(dn, pPassword);
                if (connection.isAuthenticated()) {
                    email = getEmail(connection, dn, pLogin);
                    accessGranted = true;
                } else {
                    errorMessage = String.format("LDAP Authentication failed for user %s", pLogin);
                }
            }
        } catch (final LdapAuthenticationException e) {
            LOGGER.error("LDAP authentication error returned by the LDAP server [{}]: {}",
                         e.getResultCode(),
                         e.getMessage());
            errorMessage = e.getMessage();
        } catch (final LdapException | IOException e) {
            LOGGER.error("LDAP error : " + e.getMessage(), e);
            errorMessage = e.getMessage();
        } finally {
            try {
                if (connection.isAuthenticated()) {
                    connection.unBind();
                }
            } catch (final LdapException e) {
                LOGGER.error("ERROR during LDAP unbind : " + e.getMessage(), e);
            }
        }

        // Return connection state
        return new AuthenticationPluginResponse(accessGranted, email, errorMessage);

    }

    /**
     * Retrieve LDAP connection
     *
     * @param pHost ldap server host
     * @param pPort ldap server port
     * @return LdapConnection
     */
    public LdapConnection getLdapConnection(final String pHost, final Integer pPort) {
        return new LdapNetworkConnection(pHost, pPort);
    }

    /**
     * Retrieve user name from ldap server
     *
     * @param pLdapContext ldap connection
     * @param pDn          ldap dn
     * @param pLogin       User login
     * @return user email
     * @throws LdapException error during LDAP transation
     */
    private String getEmail(final LdapConnection pLdapContext, final String pDn, final String pLogin)
        throws LdapException {

        final EntryCursor cursor;
        String userMail = null;
        boolean userFound = false;
        final String searchFilter = "(&" + ldapSearchUserFilter + "(" + ldapUserLoginAttribute + "=" + pLogin + "))";
        try {

            LOGGER.info("LDAP DN=" + pDn);
            LOGGER.info("SEARCH FILTER=" + searchFilter);
            LOGGER.info("SEARCH SCOPE=" + SearchScope.SUBTREE);
            LOGGER.info("EMAIL ATTRIBUTE=" + ldapEmailAttribute);
            cursor = pLdapContext.search(pDn, searchFilter, SearchScope.SUBTREE, ldapEmailAttribute);

            while (cursor.next() && (userMail == null)) {
                userFound = true;
                final Entry entry = cursor.get();
                final Attribute attribute = entry.get(ldapEmailAttribute);
                if ((attribute != null) && (attribute.getString() != null)) {
                    userMail = attribute.getString();
                }
            }
        } catch (final LdapException | CursorException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        if (!userFound) {
            throw new LdapException(String.format("Unable to find user %s from LDAP Server with request %s.",
                                                  pLogin,
                                                  searchFilter));
        }

        if ((userMail == null) || userMail.isEmpty()) {
            throw new LdapException(String.format("No valid email returned by LDAP server for user %s.", pLogin));
        }

        return userMail;

    }
}
