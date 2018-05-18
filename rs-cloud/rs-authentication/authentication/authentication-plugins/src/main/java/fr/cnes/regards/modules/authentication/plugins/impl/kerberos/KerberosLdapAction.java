/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.plugins.impl.kerberos;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Objects;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.authentication.plugins.impl.ldap.LdapAuthenticationPlugin;

/**
 *
 * Class KerberosLdapAction
 *
 * JAAS Kerberos action to retrieve informations from LDAP.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class KerberosLdapAction implements PrivilegedAction<DirContext> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LdapAuthenticationPlugin.class);

    /**
     * LDAP Server name
     */
    private final String serverName;

    /**
     * User login
     */
    private final String userLogin;

    /**
     * LDAP Server port
     */
    private final int serverPort;

    /**
     * Racine du contexte LDAP pour la recherche.
     */
    private String ldapRoot;

    /**
     * Contexte du LDAP.
     */
    private DirContext context;

    public KerberosLdapAction(final String pServerName, final int pServerPort, final String pUserLogin) {
        super();
        serverName = pServerName;
        serverPort = pServerPort;
        userLogin = pUserLogin;
    }

    /**
     *
     * {@inheritDoc}
     *
     * @return
     * @see java.security.PrivilegedAction#run()
     */
    @Override
    public DirContext run() {
        try {
            context = connect();
        } catch (final LdapException e) {
            LOG.error(e.getMessage(), e);
        }
        return context;
    }

    /**
     *
     * Connect to LDAP server using kerberos ticket
     *
     * @return {@link DirContext}
     * @throws LdapException
     *             Connection error
     * @since 1.0-SNAPSHOT
     */
    public DirContext connect() throws LdapException {

        try {
            final Hashtable<String, String> env = new Hashtable<>(); // NOSONAR

            // Initialisation des variables d'environnement
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

            // Fournit l'adresse du serveur

            final String canonicalName = InetAddress.getByName(serverName).getCanonicalHostName();
            extractDomainRoot(canonicalName);
            final String fullAddress = "ldap://" + canonicalName + ":" + serverPort;
            env.put(Context.PROVIDER_URL, fullAddress);

            // Indique l'authentification par le mécanisme SASL "GSSAPI" (Kerberos
            // v5)
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            context = new InitialDirContext(env);
            return context;
        } catch (NamingException | UnknownHostException e) {
            LOG.error(e.getMessage(), e);
            throw new LdapException(e.getMessage());
        }
    }

    /**
     * Construct Root LDAP server name from complete LDAP domain name
     *
     * @param pFullLdapDomainName
     *            complete LDAP domain name
     */
    private void extractDomainRoot(final String pFullLdapDomainName) {
        if (ldapRoot == null) {
            final int dotIndex = pFullLdapDomainName.indexOf('.');
            final String domain = pFullLdapDomainName.substring(dotIndex + 1);
            final String[] parts = domain.split("\\.");
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                builder.append("DC=" + parts[i].toUpperCase());
                if (i < (parts.length - 1)) {
                    builder.append(",");
                }
            }
            ldapRoot = builder.toString();
        }
    }

    /**
     *
     * Retrieve user email from LDAP server
     *
     * @param pLdapRootDn
     *            Root DN
     * @param pLdapEmailAttribute
     *            email attribute to retrieve from User object
     * @param pUserFilter
     *            LDAP Filter to find User object
     * @param pAccountNameLabel
     *            Account name attribute from User object
     * @return account mail
     * @throws LdapException
     *             Error getting user mail
     * @since 1.0-SNAPSHOT
     */
    public String getUserEmail(final String pLdapRootDn, final String pLdapEmailAttribute, final String pUserFilter,
            final String pAccountNameLabel) throws LdapException {
        String userMail = null;

        if (context == null) {
            throw new LdapException("No kerberos context to contact LDAP server.");
        }

        try {
            final String searchFilter = "(&" + pUserFilter + "(" + pAccountNameLabel + "=" + userLogin + "))";
            final String[] attrIDs = { pLdapEmailAttribute };
            final SearchControls controls = setControls(attrIDs);
            final NamingEnumeration<SearchResult> answer = context.search(pLdapRootDn, searchFilter, controls);
            if (answer.hasMore()) {
                final Attributes attrs = answer.next().getAttributes();
                final javax.naming.directory.Attribute emailAttr = attrs.get(pLdapEmailAttribute);
                if ((emailAttr != null) && (emailAttr.get() != null)) {
                    userMail = emailAttr.get().toString();
                }
            }
        } catch (final NamingException e) {
            LOG.error(e.getMessage(), e);
        }

        if ((userMail == null) || userMail.isEmpty()) {
            throw new LdapException("No valid email returned by LDAP server");
        }

        return userMail;

    }

    /**
     * Initialize LDAP search controls
     *
     * @param pArgs
     *            search arguments
     * @return {@link SearchControls}
     */
    public SearchControls setControls(final String[] pArgs) {
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if (Objects.nonNull(pArgs)) {
            controls.setReturningAttributes(pArgs);
        }
        return controls;
    }
}
