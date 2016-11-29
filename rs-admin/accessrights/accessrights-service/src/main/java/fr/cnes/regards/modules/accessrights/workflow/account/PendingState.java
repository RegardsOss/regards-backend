/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status PENDING.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class PendingState implements IAccountTransitions {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PendingState.class);

    /**
     * The email validation template code
     */
    private static final String EMAIL_VALIDATION_TEMPLATE_CODE = "emailValidationTemplate";

    /**
     * Account Repository. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * Account Settings Service. Autowired by Spring.
     */
    private final IAccountSettingsService accountSettingsService;

    // /**
    // * Template Service. Autowired by Spring.
    // */
    // private final ITemplateService templateService;
    //
    // /**
    // * Email Service. Autowired by Spring.
    // */
    // private final IEmailService emailService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new PENDING state
     *
     * @param pAccountRepository
     *            the account repository
     * @param pAccountSettingsService
     *            the account settings repository
     */
    public PendingState(final IAccountRepository pAccountRepository,
            final IAccountSettingsService pAccountSettingsService) {
        super();
        accountRepository = pAccountRepository;
        accountSettingsService = pAccountSettingsService;
        // templateService = pTemplateService;
        // emailService = pEmailService;
    }

    @Override
    public void makeAdminDecision(final Account pAccount, final boolean pAccepted) throws EntityNotFoundException {
        final AccountSettings settings = accountSettingsService.retrieve();

        if (pAccepted || "auto-accept".equals(settings.getMode())) {
            // Change the status of the account
            pAccount.setStatus(AccountStatus.ACCEPTED);
            accountRepository.save(pAccount);

            // sendValidationEmail(pAccount);

        } else {
            accountRepository.delete(pAccount);
        }
    }

    // /**
    // * Send the validation email to the passed account
    // *
    // * @param pAccount
    // * the account to whom send the validation email
    // * @throws EntityNotFoundException
    // * if the email validation template could not be found
    // */
    // private void sendValidationEmail(final Account pAccount) throws EntityNotFoundException {
    // // Create the validation email from a template
    // final String[] recipients = { pAccount.getEmail() };
    // // TODO
    // final Map<String, String> data = new HashMap<>();
    // data.put("name", pAccount.getFirstName());
    // data.put("email", pAccount.getEmail());
    // data.put("code", pAccount.getCode());
    // final SimpleMailMessage email = templateService.writeToEmail(EMAIL_VALIDATION_TEMPLATE_CODE, data, recipients);
    //
    // // Send it
    // emailService.sendEmail(email);
    // }

}
