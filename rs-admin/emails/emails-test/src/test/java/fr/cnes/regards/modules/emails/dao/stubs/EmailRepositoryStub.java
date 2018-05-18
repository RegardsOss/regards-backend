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
package fr.cnes.regards.modules.emails.dao.stubs;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

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
