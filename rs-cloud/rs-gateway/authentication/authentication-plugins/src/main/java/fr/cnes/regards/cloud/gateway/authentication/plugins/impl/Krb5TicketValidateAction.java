/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl;

import java.security.PrivilegedExceptionAction;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

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
    public static final String KERB_V5_OID = "1.2.840.113554.1.2.2"; //$NON-NLS-1$

    /**
     * Kerberos ticket to validate
     */
    private final byte[] ticket;

    private final String spn;

    public Krb5TicketValidateAction(final byte[] ticket, final String spn) {
        this.ticket = ticket;
        this.spn = spn;
    }

    @Override
    public String run() throws Exception {
        final Oid krbOid = new Oid(KERB_V5_OID);

        final GSSManager gssmgr = GSSManager.getInstance();

        // tell the GSSManager the Kerberos name of the service
        final GSSName serviceName = gssmgr.createName(spn, null);

        // get the service's credentials. note that this run() method was called by Subject.doAs(),
        // so the service's credentials (Service Principal Name and password) are already
        // available in the Subject
        final GSSCredential serviceCredentials = gssmgr.createCredential(serviceName, GSSCredential.INDEFINITE_LIFETIME,
                                                                         krbOid, GSSCredential.ACCEPT_ONLY);

        // create a security context for decrypting the service ticket
        final GSSContext gssContext = gssmgr.createContext(serviceCredentials);

        // decrypt the service ticket
        gssContext.acceptSecContext(ticket, 0, ticket.length);

        // get the client name from the decrypted service ticket
        // note that Active Directory created the service ticket, so we can trust it
        final String clientName = gssContext.getSrcName().toString();

        // clean up the context
        gssContext.dispose();

        // return the authenticated client name
        return clientName;
    }

}
