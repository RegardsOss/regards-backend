/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.account;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Throwables;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.registration.IVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * State class of the State Pattern implementing the available actions on a {@link Account} in status PENDING.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
@InstanceTransactional
public class PendingState extends AbstractDeletableState {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PendingState.class);

    /**
     * Account Repository. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * CRUD service handling {@link Template}s. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * CRUD service handling {@link VerificationToken}s. Autowired by Spring.
     */
    private final IVerificationTokenService tokenService;

    /**
     * Creates a new PENDING state
     *
     * @param pAccountRepository
     * @param pTemplateService
     * @param pEmailClient
     * @param pTokenService
     * @since 1.0-SNAPSHOT
     */
    public PendingState(final IAccountRepository pAccountRepository, final ITemplateService pTemplateService, // NOSONAR
            final IEmailClient pEmailClient, final IVerificationTokenService pTokenService,
            final IProjectUserService pProjectUserService, final ITenantResolver pTenantResolver,
            final IRuntimeTenantResolver pRuntimeTenantResolver, final IPasswordResetService pPasswordResetTokenService,
            final IVerificationTokenService pVerificationTokenService) {
        super(pProjectUserService, pAccountRepository, pTenantResolver, pRuntimeTenantResolver,
              pPasswordResetTokenService, pVerificationTokenService);
        accountRepository = pAccountRepository;
        templateService = pTemplateService;
        emailClient = pEmailClient;
        tokenService = pTokenService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions#acceptAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void acceptAccount(final Account pAccount) throws EntityException {
        pAccount.setStatus(AccountStatus.ACCEPTED);
        accountRepository.save(pAccount);
        sendValidationEmail(pAccount);
    }

    /**
     * Send the email for account validation.
     *
     * @param pAccount
     *            the account
     * @throws EntityNotFoundException
     *             when no verification token linked to the account could be found
     */
    public void sendValidationEmail(final Account pAccount) throws EntityNotFoundException {
        // Retrieve the token
        final VerificationToken token = tokenService.findByAccount(pAccount);

        // Build the list of recipients
        final String[] recipients = { pAccount.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", pAccount.getFirstName());

        String linkUrlTemplate;
        if ((token != null) && token.getRequestLink().contains("?")) {
            linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
        } else {
            linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
        }
        final String confirmationUrl;
        try {
            confirmationUrl = String.format(linkUrlTemplate, token.getRequestLink(), UriUtils.encode(token.getOriginUrl(),
                                                                                             StandardCharsets.UTF_8.name()),
                                    token.getToken(), pAccount.getEmail());
            data.put("confirmationUrl", confirmationUrl);
        } catch (UnsupportedEncodingException e) {
            LOG.error("This system does not support UTF-8",e);
            throw new RuntimeException(e);//NOSONAR: this should only be a development error, if it happens the system has to explode
        }


        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(TemplateServiceConfiguration.EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE,
                                                 data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template for registration confirmation. Falling back to default template.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Registration Confirmation");
            email.setText("Please click on the following link to confirm your registration: " + confirmationUrl);
        }

        // Send it
        emailClient.sendEmail(email);
    }

}
