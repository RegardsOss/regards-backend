/*
 * LICENSE_PLACEHOLDER
 */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 10/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.cloud.gateway.authentication.plugins.impl.ldap;

import java.io.IOException;

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

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * Class LdapAuthenticationPlugin LDAP Authentication plugin.
 *
 * @author Sébastien Binda
 * @since 1.0
 */
@Plugin(description = "LDAP authentication plugin", id = "LdapAuthenticationPlugin", version = "1.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
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
    @PluginParameter(name = PARAM_LDAP_HOST, description = "LDAP Server address")
    private String ldapHost;

    /**
     * LDAP Port
     */
    @PluginParameter(name = PARAM_LDAP_PORT, description = "LDAP Server port (default 389)")
    private String ldapPort;

    /**
     * LDAP DN
     */
    @PluginParameter(name = PARAM_LDAP_CN, description = "LDAP Root CN")
    private String ldapDN;

    /**
     * LDAP User login attribute.
     */
    @PluginParameter(name = PARAM_LDAP_USER_LOGIN_ATTTRIBUTE,
            description = "LDAP User parameter containing user login (default=sAMAccountLogin)")
    private String ldapUserLoginAttribute;

    /**
     * LDAP Filter to find the User object
     */
    @PluginParameter(name = PARAM_LDAP_USER_FILTER_ATTTRIBUTE,
            description = "LDAP Filter to find the user object (default = (ObjectClass=user)")
    private String ldapSearchUserFilter;

    /**
     * LDAP email attribute label
     */
    @PluginParameter(name = PARAM_LDAP_USER_EMAIL_ATTTRIBUTE,
            description = "LDAP parameter for user email (default=mail)")
    private String ldapEmailAttribute;

    /**
     * Initialize default values
     *
     * @since 1.0-SNAPSHOT
     */
    @PluginInit
    public void setDefaultValues() {
        if (ldapSearchUserFilter == null) {
            ldapSearchUserFilter = "(ObjectClass=user)";
        }

        if (ldapPort == null) {
            ldapPort = "389";
        }

        if (ldapEmailAttribute == null) {
            ldapEmailAttribute = "mail";
        }

    }

    /**
     * Overridden method
     *
     * @see fr.cs.regards.interfaces.plugins.IAuthenticationPlugin#authenticate(java.lang.String, java.lang.String,
     * java.lang.String)
     * @since 1.0
     */
    @Override
    public AuthenticationPluginResponse authenticate(final String pLogin, final String pPassword, final String pScope) {

        AuthenticationStatus status = AuthenticationStatus.ACCESS_DENIED;
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
                status = AuthenticationStatus.ACCESS_DENIED;
                errorMessage = "Connection failed to LDAP Server";
            } else {
                connection.bind(dn, pPassword);
                if (connection.isAuthenticated()) {
                    email = getEmail(connection, dn, pLogin);
                    status = AuthenticationStatus.ACCESS_GRANTED;
                } else {
                    errorMessage = String.format("LDAP Authentication failed for user %s", pLogin);
                }
            }
        } catch (final LdapAuthenticationException e) {
            LOGGER.error("LDAP authentication error : " + e.getMessage(), e);
            status = AuthenticationStatus.ACCESS_DENIED;
            errorMessage = e.getMessage();
        } catch (final LdapException | IOException e) {
            LOGGER.error("LDAP error : " + e.getMessage(), e);
            status = AuthenticationStatus.ACCESS_DENIED;
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
        return new AuthenticationPluginResponse(status, email, errorMessage);

    }

    /**
     * Retrieve LDAP connection
     *
     * @param pHost ldap server host
     * @param pPort ldap server port
     * @return LdapConnection
     * @since 1.0-SNAPSHOT
     */
    public LdapConnection getLdapConnection(final String pHost, final Integer pPort) {
        return new LdapNetworkConnection(pHost, pPort);
    }

    /**
     * Retrieve user name from ldap server
     *
     * @param pLdapContext ldap connection
     * @param pDn ldap dn
     * @param pLogin User login
     * @return user email
     * @throws LdapException error during LDAP transation
     * @since 1.0-SNAPSHOT
     */
    private String getEmail(final LdapConnection pLdapContext, final String pDn, final String pLogin)
            throws LdapException {

        final EntryCursor cursor;
        String userMail = null;
        final String searchFilter = "(&" + ldapSearchUserFilter + "(" + ldapUserLoginAttribute + "=" + pLogin + "))";
        try {
            cursor = pLdapContext.search(pDn, searchFilter, SearchScope.SUBTREE, ldapEmailAttribute);

            while (cursor.next() && (userMail == null)) {
                final Entry entry = cursor.get();
                final Attribute attribute = entry.get(ldapEmailAttribute);
                if ((attribute != null) && (attribute.getString() != null)) {
                    userMail = attribute.getString();
                }
            }
        } catch (final LdapException | CursorException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        if ((userMail == null) || userMail.isEmpty()) {
            throw new LdapException(String.format("No valid email returned by LDAP server for user %s.", pLogin));
        }

        return userMail;

    }
}
