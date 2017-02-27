/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.accountunlock.IAccountUnlockTokenService;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.AccountUnlockToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.PasswordResetToken;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status LOCKED.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class LockedState extends AbstractDeletableState {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LockedState.class);

    /**
     * The account unlock email template code
     */
    private static final String ACCOUNT_UNLOCK_RESET_TEMPLATE = "accountUnlockTemplate";

    /**
     * CRUD service handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountService accountService;

    /**
     * CRUD service handling {@link PasswordResetToken}s. Autowired by Spring.
     */
    private final IAccountUnlockTokenService tokenService;

    /**
     * Template Service. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param pProjectUserService
     * @param pAccountRepository
     * @param pTenantResolver
     * @param pRuntimeTenantResolver
     * @param pAccountService
     * @param pTokenService
     * @param pTemplateService
     * @param pEmailClient
     */
    public LockedState(final IProjectUserService pProjectUserService, final IAccountRepository pAccountRepository,
            final ITenantResolver pTenantResolver, final IRuntimeTenantResolver pRuntimeTenantResolver,
            final IAccountService pAccountService, final IAccountUnlockTokenService pTokenService,
            final ITemplateService pTemplateService, final IEmailClient pEmailClient) {
        super(pProjectUserService, pAccountRepository, pTenantResolver, pRuntimeTenantResolver);
        accountService = pAccountService;
        tokenService = pTokenService;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#requestUnlockAccount(fr.cnes.regards.
     * modules.accessrights.accountunlock.OnAccountUnlockEvent)
     */
    @Override
    public void requestUnlockAccount(final Account pAccount, final String pOriginUrl, final String pRequestLink)
            throws EntityOperationForbiddenException {
        // Create the token
        final String token = UUID.randomUUID().toString();
        tokenService.create(pAccount);

        // Build the list of recipients
        final String[] recipients = { pAccount.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", pAccount.getFirstName());
        data.put("requestLink", pRequestLink);
        data.put("originUrl", pOriginUrl);
        data.put("token", token);
        data.put("accountEmail", pAccount.getEmail());

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(ACCOUNT_UNLOCK_RESET_TEMPLATE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template to generate a password reset email. Falling back to default.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Password Reset");

            final String linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
            final String linkUrl = String.format(linkUrlTemplate, pRequestLink, pOriginUrl, token, pAccount.getEmail());
            email.setText("Please click on the following link to set a new password for your account: " + linkUrl);
        }

        // Send it
        emailClient.sendEmail(email);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#performUnlockAccount(fr.cnes.regards.
     * modules.accessrights.domain.instance.Account, java.lang.String)
     */
    @Override
    public void performUnlockAccount(final Account pAccount, final String pToken) throws EntityException {
        validateToken(pAccount.getEmail(), pToken);
        accountService.updateAccount(pAccount.getId(), pAccount);
    }

    /**
     * Validate the token
     *
     * @param pAccountEmail
     *            the account email
     * @param pToken
     *            the token to validate
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} when the token is not linked to the passed account or is
     *             expired<br>
     *             {@link EntityNotFoundException} when the token dos not exist
     */
    private void validateToken(final String pAccountEmail, final String pToken) throws EntityException {
        // Retrieve the token object
        final AccountUnlockToken token = tokenService.findByToken(pToken);

        // Check same account
        if (!token.getAccount().getEmail().equals(pAccountEmail)) {
            throw new EntityOperationForbiddenException(pToken, PasswordResetToken.class, "Invalid token");
        }

        // Check token expiry
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EntityOperationForbiddenException(pToken, PasswordResetToken.class, "Expired token");
        }
    }

}
