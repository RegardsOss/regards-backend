/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountWorkflowManager;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.ITemplateService;

/**
 * {@link IRegistrationService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
public class RegistrationService implements IRegistrationService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    /**
     * The email validation template code
     */
    private static final String EMAIL_VALIDATION_TEMPLATE_CODE = "emailValidationTemplate";

    /**
     * CRUD repository handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * CRUD repository handling {@link Role}s. Autowired by Spring.
     */
    private final IRoleService roleService;

    /**
     * CRUD service handling {@link VerificationToken}s. Autowired by Spring.
     */
    @Autowired
    private final IVerificationTokenService tokenService;

    /**
     * CRUD repository handling {@link AccountSettingst}s. Autowired by Spring.
     */
    private final IAccountSettingsService accountSettingsService;

    /**
     * Account workflow manager
     */
    private final AccountWorkflowManager accountWorkflowManager;

    /**
     * CRUD service handling {@link Template}s. Autowired by Spring.
     */
    private final ITemplateService templateService;

    /**
     * Email Client. Autowired by Spring.
     */
    private final IEmailClient emailClient;

    /**
     * @param pAccountRepository
     *            the account repository
     * @param pProjectUserRepository
     *            the project user repository
     * @param pRoleService
     *            the role service
     * @param pTokenService
     *            the token service
     * @param pAccountSettingsService
     *            the account settings service
     * @param pAccountWorkflowManager
     *            the account workflow manager
     * @param pTemplateService
     *            the template service
     * @param pEmailClient
     *            the email client
     */
    public RegistrationService(final IAccountRepository pAccountRepository,
            final IProjectUserRepository pProjectUserRepository, final IRoleService pRoleService,
            final IVerificationTokenService pTokenService, final IAccountSettingsService pAccountSettingsService,
            final AccountWorkflowManager pAccountWorkflowManager, final ITemplateService pTemplateService,
            final IEmailClient pEmailClient) {
        super();
        accountRepository = pAccountRepository;
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        tokenService = pTokenService;
        accountSettingsService = pAccountSettingsService;
        accountWorkflowManager = pAccountWorkflowManager;
        templateService = pTemplateService;
        emailClient = pEmailClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions#requestAccount(fr.cnes.regards.modules.
     * accessrights.domain.AccessRequestDTO)
     */
    @Override
    public void requestAccess(final AccessRequestDto pDto) throws EntityException {
        // Create the account if needed
        try {
            requestAccount(pDto);
        } catch (final EntityException e) {
            LOG.info("Requesting access with an existing account. Ok, no account created", e);
        }

        // Create the project user
        requestProjectUser(pDto);
    }

    /**
     * Create the account
     *
     * @param pDto
     * @return
     * @throws EntityException
     */
    private Account requestAccount(final AccessRequestDto pDto) throws EntityException {
        // Check existence
        if (accountRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new EntityAlreadyExistsException("The email " + pDto.getEmail() + " is already in use.");
        }
        // Create the new account
        final Account account = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(),
                pDto.getPassword());

        // Check status
        Assert.isTrue(AccountStatus.PENDING.equals(account.getStatus()),
                      "Trying to create an Account with other status than PENDING.");

        // Auto-accept if configured so
        final AccountSettings settings = accountSettingsService.retrieve();
        if (AccountSettings.AUTO_ACCEPT_MODE.equals(settings.getMode())) {
            accountWorkflowManager.acceptAccount(account);
        }

        // Create
        accountRepository.save(account);

        // Init the verification token
        tokenService.create(account, pDto.getOriginUrl(), pDto.getRequestLink());

        return account;
    }

    /**
     * Create the project user
     *
     * @param pDto
     * @throws EntityException
     */
    private void requestProjectUser(final AccessRequestDto pDto) throws EntityException {
        // Check that an associated account exitsts
        if (!accountRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new EntityNotFoundException(pDto.getEmail(), Account.class);
        }

        // Check that no project user with same email exists
        if (projectUserRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new EntityAlreadyExistsException("The email " + pDto.getEmail() + "is already in use.");
        }

        // Init with default role
        final Role role = roleService.getDefaultRole();

        // Create a new project user
        final ProjectUser projectUser = new ProjectUser(pDto.getEmail(), role, new ArrayList<>(), new ArrayList<>());

        // Check the status
        Assert.isTrue(UserStatus.WAITING_ACCESS.equals(projectUser.getStatus()),
                      "Trying to create a ProjectUser with other status than WAITING_ACCESS.");

        // Save
        projectUserRepository.save(projectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.registration.IRegistrationService#sendValidationEmail(fr.cnes.regards.
     * modules.accessrights.registration.OnAcceptAccountEvent)
     */
    @Override
    public void sendValidationEmail(final Account pAccount) throws EntityNotFoundException {
        // Retrieve the token
        final VerificationToken token = tokenService.findByAccount(pAccount);

        // Build the list of recipients
        final String[] recipients = { pAccount.getEmail() };

        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", pAccount.getFirstName());
        data.put("originUrl", token.getOriginUrl());
        data.put("requestLink", token.getRequestLink());
        data.put("token", token.getToken());
        data.put("accountEmail", pAccount.getEmail());

        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(EMAIL_VALIDATION_TEMPLATE_CODE, data, recipients);
        } catch (final EntityNotFoundException e) {
            LOG.warn("Could not find the template for registration confirmation. Falling back to default template.", e);
            email = new SimpleMailMessage();
            email.setTo(recipients);
            email.setSubject("REGARDS - Registration Confirmation");

            final String linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
            final String linkUrl = String.format(linkUrlTemplate, token.getRequestLink(), token.getOriginUrl(),
                                                 token.getToken(), pAccount.getEmail());
            email.setText("Please click on the following link to confirm your registration: " + linkUrl);
        }

        // Send it
        emailClient.sendEmail(email);
    }

}
