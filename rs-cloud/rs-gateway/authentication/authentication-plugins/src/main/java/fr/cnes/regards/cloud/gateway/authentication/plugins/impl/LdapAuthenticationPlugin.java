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

import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
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
@Plugin(author = "CS-SI", description = "LDAP authentication plugin", id = "LDAPAuthenticationPlugin", version = "1.0")
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
     * Overridden method
     *
     * @see fr.cs.regards.interfaces.plugins.IAuthenticationPlugin#authenticate(java.lang.String, java.lang.String,
     *      java.lang.String)
     * @since 1.0
     */
    @Override
    public AuthenticationPluginResponse authenticate(final String pName, final String pPassword, final String pScope) {

        final AuthenticationPluginResponse response = new AuthenticationPluginResponse();
        // Create LDAP Connection
        final LdapConnection connection = new LdapNetworkConnection(ldapHost, Integer.parseInt(ldapPort));

        // Create DN parameter with login in uid parameter.
        String dn = "uid=" + pName;
        if ((ldapDN != null) && !ldapDN.isEmpty()) {
            dn = dn + "," + ldapDN;
        }

        try {
            // Check connection
            if (!connection.connect()) {
                response.setStatus(AuthenticationStatus.ACCESS_DENIED);
                response.setErrorMessage("Error connecting to LDAP Server");
            } else {
                connection.bind(dn, pPassword);
                if (connection.isAuthenticated()) {
                    response.setStatus(AuthenticationStatus.ACCESS_GRANTED);
                } else {
                    response.setStatus(AuthenticationStatus.ACCESS_DENIED);
                }
            }
        } catch (final LdapAuthenticationException e) {
            LOGGER.error("LDAP authentication error : " + e.getMessage(), e);
            response.setStatus(AuthenticationStatus.ACCESS_DENIED);
            response.setErrorMessage(e.getMessage());
        } catch (final LdapException | IOException e) {
            LOGGER.error("LDAP error : " + e.getMessage(), e);
            response.setStatus(AuthenticationStatus.ACCESS_DENIED);
            response.setErrorMessage(e.getMessage());
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
        return response;

    }
}
