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

import java.security.PrivilegedExceptionAction;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Class Krb5TicketValidateAction
 *
 * Handler to validate a kerberos ticket
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class Krb5TicketValidateAction implements PrivilegedExceptionAction<String> {

    /**
     * OID Kerberos V5
     */
    public static final String KERB_V5_OID = "1.2.840.113554.1.2.2";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Krb5TicketValidateAction.class);

    /**
     * Kerberos ticket to validate
     */
    private final byte[] ticket;

    /**
     * REGARDS ticket service
     */
    private final String principal;

    /**
     * Context generated for the ticket validation
     */
    private GSSContext gssContext = null;

    public Krb5TicketValidateAction(final byte[] pTicket, final String pPrincipal) {
        this.ticket = pTicket;
        this.principal = pPrincipal;
    }

    @Override
    public String run() throws Exception {
        final Oid krbOid = new Oid(KERB_V5_OID);

        final GSSManager gssmgr = GSSManager.getInstance();

        // tell the GSSManager the Kerberos name of the service
        final GSSName serviceName = gssmgr.createName(principal, null);

        // get the service's credentials. note that this run() method was called by Subject.doAs(),
        // so the service's credentials (Service Principal Name and password) are already
        // available in the Subject
        final GSSCredential serviceCredentials = gssmgr.createCredential(serviceName, GSSCredential.INDEFINITE_LIFETIME,
                                                                         krbOid, GSSCredential.ACCEPT_ONLY);

        // create a security context for decrypting the service ticket
        gssContext = gssmgr.createContext(serviceCredentials);

        // decrypt the service ticket
        gssContext.acceptSecContext(ticket, 0, ticket.length);

        // get the client name from the decrypted service ticket
        // note that Active Directory created the service ticket, so we can trust it
        return gssContext.getSrcName().toString();
    }

    /**
     *
     * Retrieve authenticated kerberos context
     *
     * @return {@link GSSContext}
     * @since 1.0-SNAPSHOT
     */
    public GSSContext getGssContext() {
        return gssContext;
    }

    /**
     *
     * Close Kerberos context
     *
     * @since 1.0-SNAPSHOT
     */
    public void closeContext() {
        if (gssContext != null) {
            try {
                gssContext.dispose();
            } catch (final GSSException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

}
