/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.fallback;

import java.util.List;

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

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.emails.client.IEmailClient#retrieveEmails()
     */
    @Override
    public ResponseEntity<List<Email>> retrieveEmails() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.emails.client.IEmailClient#sendEmail(org.springframework.mail.SimpleMailMessage)
     */
    @Override
    public ResponseEntity<SimpleMailMessage> sendEmail(final SimpleMailMessage pMessage) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.emails.client.IEmailClient#retrieveEmail(java.lang.Long)
     */
    @Override
    public ResponseEntity<Email> retrieveEmail(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.emails.client.IEmailClient#resendEmail(java.lang.Long)
     */
    @Override
    public void resendEmail(final Long pId) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.cnes.regards.modules.emails.client.IEmailClient#deleteEmail(java.lang.Long)
     */
    @Override
    public void deleteEmail(final Long pId) {
        // TODO Auto-generated method stub

    }

}
