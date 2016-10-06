/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.EmailDTO;

/**
 * Stub repository class for testing purposes
 *
 * @author Xavier-Alexandre Brochard
 */
@Repository
@Profile("test")
@Primary
public class EmailRepositoryStub extends JpaRepositoryStub<EmailDTO> implements IEmailRepository {

    public EmailRepositoryStub(final JavaMailSender pSender) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("The subject");
        message.setFrom("recipient@stub.com");
        message.setText("The body of the message");
        EmailDTO dto = new EmailDTO(message);
        dto.setId(0L);
        entities_.add(dto);

        message = new SimpleMailMessage();
        message.setSubject("Another subject");
        message.setFrom("another.recipient@stub.com");
        message.setText("Another body of the message");
        dto = new EmailDTO(message);
        dto.setId(1L);
        entities_.add(dto);
    }

}
