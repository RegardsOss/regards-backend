/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.accountunlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.accountunlock.IAccountUnlockTokenRepository;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * {@link IAccountUnlockService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class AccountUnlockService implements IAccountUnlockService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountUnlockService.class);

    /**
     * The account unlock email template code
     */
    private static final String ACCOUNT_UNLOCK_RESET_TEMPLATE = "accountUnlockTemplate";

    /**
     * CRUD repository handling {@link AccountUnlockToken}s. Autowired by Spring.
     */
    @Autowired
    private final IAccountUnlockTokenRepository tokenRepository;

    /**
     * Template Service. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param pTokenRepository
     *            the token repository
     * @param pTemplateService
     *            the template repository
     * @param pEmailClient
     *            the email client
     */
    public AccountUnlockService(final IAccountUnlockTokenRepository pTokenRepository,
            final ITemplateService pTemplateService, final IEmailClient pEmailClient) {
        super();
        tokenRepository = pTokenRepository;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    @Override
    public void sendAccountUnlockEmail(final Account pAccount, final String pAppUrl) {
        final String token = UUID.randomUUID().toString();
        createAccountUnlockToken(pAccount, token);

        // Create the password reset email from a template
        final String[] recipients = { pAccount.getEmail() };
        final String accountUnlockUrl = pAppUrl + "/passwordReset/" + token;
        final Map<String, String> data = new HashMap<>();

        data.put("name", pAccount.getFirstName());
        data.put("accountUnlockUrl", accountUnlockUrl);
        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(ACCOUNT_UNLOCK_RESET_TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template to generate the account unlock email. Falling back to default.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Account Unlcok");
            email.setText("Please click on the following link to unlock your account: " + accountUnlockUrl);
        }

        // Send it
        emailClient.sendEmail(email);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.accountunlock.IAccountUnlockService#getAccountUnlockToken(java.lang.String)
     */
    @Override
    public AccountUnlockToken getAccountUnlockToken(final String pToken) throws EntityNotFoundException {
        return tokenRepository.findByToken(pToken)
                .orElseThrow(() -> new EntityNotFoundException(pToken, AccountUnlockToken.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.accountunlock.IAccountUnlockService#createAccountUnlockToken(fr.cnes.regards
     * .modules.accessrights.domain.instance.Account, java.lang.String)
     */
    @Override
    public void createAccountUnlockToken(final Account pAccount, final String pToken) {
        final AccountUnlockToken token = new AccountUnlockToken(pToken, pAccount);
        tokenRepository.save(token);
    }

}
