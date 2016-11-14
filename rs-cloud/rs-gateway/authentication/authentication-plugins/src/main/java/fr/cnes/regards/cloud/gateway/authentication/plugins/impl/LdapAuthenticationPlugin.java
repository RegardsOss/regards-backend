/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 10/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.cloud.gateway.authentication.plugins.impl;

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
import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;

/**
 *
 * Class LdapAuthenticationPlugin
 *
 * LDAP Authentication plugin.
 *
 * @author Sébastien Binda
 * @since 1.0
 */
@Plugin(author = "CS-SI", description = "LDAP authentication plugin", id = "LdapAuthenticationPlugin", version = "1.0")
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
     * LDAP Email attribute to retrieve
     */
    public static final String PARAM_LDAP_EMAIL_ATTTRIBUTE = "ldapEmail";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAuthenticationPlugin.class);

    /**
     * LDAP Host access
     */
    @PluginParameter(name = PARAM_LDAP_HOST)
    private String ldapHost;

    /**
     * LDAP Port
     */
    @PluginParameter(name = PARAM_LDAP_PORT)
    private String ldapPort;

    /**
     * LDAP DN
     */
    @PluginParameter(name = PARAM_LDAP_CN)
    private String ldapDN;

    /**
     * LDAP email attribute name
     */
    @PluginParameter(name = PARAM_LDAP_EMAIL_ATTTRIBUTE)
    private String ldapEmailAttribute;

    /**
     * Overridden method
     *
     * @see fr.cs.regards.interfaces.plugins.IAuthenticationPlugin#authenticate(java.lang.String, java.lang.String,
     *      java.lang.String)
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
     *
     * Retrieve LDAP connection
     *
     * @param pHost
     *            ldap server host
     * @param pPort
     *            ldap server port
     * @return LdapConnection
     * @since 1.0-SNAPSHOT
     */
    public LdapConnection getLdapConnection(final String pHost, final Integer pPort) {
        return new LdapNetworkConnection(pHost, pPort);
    }

    /**
     *
     * Retrieve user name from ldap server
     *
     * @param ctx
     *            ldap connection
     * @param pDn
     *            ldap dn
     * @return user email
     * @throws LdapException
     * @since 1.0-SNAPSHOT
     */
    private String getEmail(final LdapConnection ctx, final String pDn, final String pLogin) throws LdapException {
        EntryCursor cursor;
        String userMail = null;
        try {
            cursor = ctx.search(pDn, "(objectclass=*)", SearchScope.SUBTREE, ldapEmailAttribute);

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
