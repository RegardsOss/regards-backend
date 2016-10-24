/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.fallback;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Hystrix fallback for Feign {@link IEmailClient}. This default implementation is executed when the circuit is open or
 * there is an error.<br>
 * To enable this fallback, set the fallback attribute to this class name in {@link IEmailClient}.
 *
 * @author CS SI
 */
@Component
public class EmailFallback implements IEmailClient {

    @Override
    public HttpEntity<List<Email>> retrieveEmails() {
        // TODO Auto-generated method stub
        return null;
    }

    // @Override
    // public HttpEntity<Email> sendEmail(final EmailWithRecipientsDTO pEmail) {
    // // TODO Auto-generated method stub
    // return null;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see
     * fr.cnes.regards.modules.emails.signature.IEmailSignature#sendEmail(org.springframework.mail.SimpleMailMessage)
     */
    @Override
    public ResponseEntity<SimpleMailMessage> sendEmail(final SimpleMailMessage pMessage) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Email> retrieveEmail(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resendEmail(final Long pId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteEmail(final Long pId) {
        // TODO Auto-generated method stub

    }

}
