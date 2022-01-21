/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.emails.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.emails.dao.IEmailRepository;
import fr.cnes.regards.modules.emails.domain.Email;

/**
 *
 * Class SimpleEmailService
 *
 * Simple mail service doesn't persist mail entities in database. To persist entities use EmailService.
 *
 * @author Sébastien Binda

 */
@Profile("!nomail")
@Service
public class SimpleEmailService extends AbstractEmailService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEmailService.class);

    /**
     * Spring Framework interface for sending email
     */
    private final JavaMailSender mailSender;

    @Value("${regards.mails.noreply.address:regards@noreply.fr}")
    private String defaultSender;

    /**
     * Creates an {@link EmailService} wired to the given {@link IEmailRepository}.
     *
     * @param pMailSender
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public SimpleEmailService(final JavaMailSender pMailSender) {
        super();
        mailSender = pMailSender;
    }

    @Override
    public List<Email> retrieveEmails() {
        // No mail saved in SimpleMailService
        return new ArrayList<>();
    }

    @Override
    public Email sendEmail(final SimpleMailMessage pEmail) {
        sendMailWithSender(pEmail, defaultSender);
        // no Email entity
        return null;
    }

    @Override
    public Email sendEmail(SimpleMailMessage email, String attName, InputStreamSource attSource) {
        sendMailWithSender(email, attName, attSource, defaultSender);
        // no Email entity
        return null;
    }

    @Override
    public Email retrieveEmail(final Long id) {
        // Mail are not saved
        return null;
    }

    @Override
    public void resendEmail(final Long id) {
        // Mail are not saved
    }

    @Override
    public void deleteEmail(final Long id) {
        // Mail are not saved
    }

    @Override
    public boolean exists(final Long id) {
        // Mail are not saved
        return false;
    }

    @Override
    protected JavaMailSender getMailSender() {
        return mailSender;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
