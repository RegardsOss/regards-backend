/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.EmailVerificationTokenService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.service.TemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

/**
 * State class of the State Pattern implementing the available actions on a {@link ProjectUser} in status
 * WAITING_ACCESS.
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Component
public class WaitingAccessState extends AbstractDeletableState {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingAccessState.class);

    /**
     * Service handling {@link EmailVerificationToken}s. Autowired by Spring.
     */
    private final EmailVerificationTokenService tokenService;

    /**
     * Service handling CRUD operations on {@link Account}s. Autowired by Spring.
     */
    private final IAccountService accountService;

    /**
     * Service handling email templates. Autowired by Spring.
     */
    private final TemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * Constructor
     *
     * @param pProjectUserRepository
     * @param pTokenService
     * @param pAccountService
     * @param pTemplateService
     * @param pEmailClient
     */
    public WaitingAccessState(IProjectUserRepository pProjectUserRepository,
            EmailVerificationTokenService pTokenService, IAccountService pAccountService,
            TemplateService pTemplateService, IEmailClient pEmailClient) {
        super(pProjectUserRepository);
        tokenService = pTokenService;
        accountService = pAccountService;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#qualifyAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void qualifyAccess(final ProjectUser pProjectUser, final AccessQualification pQualification)
            throws EntityNotFoundException {

        switch (pQualification) {
            case GRANTED:
                pProjectUser.setStatus(UserStatus.WAITING_EMAIL_VERIFICATION);
                getProjectUserRepository().save(pProjectUser);
                sendVerificationEmail(pProjectUser);
                break;
            case REJECTED:
                doDelete(pProjectUser);
                break;
            case DENIED:
            default:
                pProjectUser.setStatus(UserStatus.ACCESS_DENIED);
                getProjectUserRepository().save(pProjectUser);
                break;
        }
    }

    /**
     * Send the email for email verification.
     *
     * @param pProjectUser
     *            the project user
     * @throws EntityNotFoundException
     *             when no verification token linked to the account could be found
     */
    public void sendVerificationEmail(final ProjectUser pProjectUser) throws EntityNotFoundException {
        // Retrieve the email
        String email = pProjectUser.getEmail();

        // Retrieve the token
        EmailVerificationToken token = tokenService.findByProjectUser(pProjectUser);

        // Retrieve the account
        Account account = accountService.retrieveAccountByEmail(email);

        // Build the list of recipients
        String[] recipients = { email };

        // Create a hash map in order to store the data to inject in the mail
        Map<String, String> data = new HashMap<>();
        data.put("name", account.getFirstName());

        String linkUrlTemplate;
        if ((token != null) && token.getRequestLink().contains("?")) {
            linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
        } else {
            linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
        }
        String confirmationUrl;
        try {
            confirmationUrl = String.format(linkUrlTemplate, token.getRequestLink(),
                                            UriUtils.encode(token.getOriginUrl(), StandardCharsets.UTF_8.name()),
                                            token.getToken(), email);
            data.put("confirmationUrl", confirmationUrl);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("This system does not support UTF-8", e);
            throw new RuntimeException(e);//NOSONAR: this should only be a development error, if it happens the system has to explode
        }

        SimpleMailMessage simpleMailMessage;
        try {
            simpleMailMessage = templateService.writeToEmail(
                                                             TemplateServiceConfiguration.EMAIL_ACCOUNT_VALIDATION_TEMPLATE_CODE,
                                                             data, recipients);
        } catch (final EntityNotFoundException e) {
            LOGGER.warn("Could not find the template for registration confirmation. Falling back to default template.",
                        e);
            simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setTo(recipients);
            simpleMailMessage.setSubject("REGARDS - Registration Confirmation");
            simpleMailMessage
                    .setText("Please click on the following link to confirm your registration: " + confirmationUrl);
        }

        // Send it
        emailClient.sendEmail(simpleMailMessage);
    }

}
