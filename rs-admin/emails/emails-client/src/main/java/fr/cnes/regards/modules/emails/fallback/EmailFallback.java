/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.fallback;

import java.util.List;

import javax.mail.internet.MimeMessage;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.emails.client.EmailClient;

/**
 * Hystrix fallback for Feign {@link EmailClient}. This default implementation is executed when the circuit is open or
 * there is an error.<br>
 * To enable this fallback, set the fallback attribute to this class name in {@link EmailClient}.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class EmailFallback implements EmailClient {

    @Override
    public HttpEntity<List<MimeMessage>> retrieveEmails() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<MimeMessage> sendEmail(final String[] pRecipients, final MimeMessage pEmail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<MimeMessage> retrieveEmail(final Long pId) {
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
