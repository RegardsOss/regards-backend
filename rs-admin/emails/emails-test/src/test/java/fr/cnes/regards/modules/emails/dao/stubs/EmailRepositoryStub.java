/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Stub repository class for testing purposes
 *
 * @author Xavier-Alexandre Brochard
 */
@Repository
@Profile("test")
@Primary
public class EmailRepositoryStub extends JpaRepositoryStub<Email> implements IEmailRepository {

    /**
     * Create an {@link EmailRepositoryStub} and populate a few emails
     */
    public EmailRepositoryStub() {

        Email email = new Email();
        email.setSubject("The subject");
        email.setFrom("recipient@stub.com");
        email.setText("The body of the message");
        email.setId(0L);
        email.setTo(new String[] { "xavier-alexandre.brochard@c-s.fr" });
        getEntities().add(email);

        email = new Email();
        email.setSubject("Another subject");
        email.setFrom("another.recipient@stub.com");
        email.setText("Another body of the message");
        email.setId(1L);
        email.setTo(new String[] { "xavier-alexandre.brochard@c-s.fr" });
        getEntities().add(email);
    }

}
