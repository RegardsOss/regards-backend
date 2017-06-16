/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import fr.cnes.regards.modules.accessrights.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountWorkflowManager;

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
    private final IVerificationTokenService tokenService;

    /**
     * CRUD repository handling {@link AccountSettingst}s. Autowired by Spring.
     */
    private final IAccountSettingsService accountSettingsService;

    /**
     * Account workflow manager. Autowired by Spring.
     */
    private final AccountWorkflowManager accountWorkflowManager;

    /**
     * @param pAccountRepository CRUD repository handling {@link Account}s. Autowired by Spring. Must no be null.
     * @param pProjectUserRepository CRUD repository handling {@link ProjectUser}s. Autowired by Spring. Must not be null.
     * @param pRoleService CRUD repository handling {@link Role}s. Autowired by Spring. Must not be null.
     * @param pTokenService  CRUD service handling {@link VerificationToken}s. Autowired by Spring. Must not be null.
     * @param pAccountSettingsService CRUD repository handling {@link AccountSettingst}s. Autowired by Spring. Must not be null.
     * @param pAccountWorkflowManagerAccount workflow manager. Autowired by Spring. Must not be null.
     */
    public RegistrationService(IAccountRepository pAccountRepository, IProjectUserRepository pProjectUserRepository,
            IRoleService pRoleService, IVerificationTokenService pTokenService,
            IAccountSettingsService pAccountSettingsService, AccountWorkflowManager pAccountWorkflowManager) {
        super();
        Assert.notNull(pAccountRepository);
        Assert.notNull(pProjectUserRepository);
        Assert.notNull(pRoleService);
        Assert.notNull(pTokenService);
        Assert.notNull(pAccountSettingsService);
        Assert.notNull(pAccountWorkflowManager);
        accountRepository = pAccountRepository;
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        tokenService = pTokenService;
        accountSettingsService = pAccountSettingsService;
        accountWorkflowManager = pAccountWorkflowManager;
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
        requestAccountIfNecessary(pDto);

        // Create the project user
        requestProjectUser(pDto);
    }

    /**
     * Create the account if necessary
     *
     * @param pDto
     * @return
     * @throws EntityException
     */
    private void requestAccountIfNecessary(final AccessRequestDto pDto) throws EntityException {
        // Check existence
        if (accountRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            LOG.info("Requesting access with an existing account. Ok, no account created");
            return;
        }

        // Create the new account
        final Account account = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(),
                EncryptionUtils.encryptPassword(pDto.getPassword()));

        // Check status
        Assert.isTrue(AccountStatus.PENDING.equals(account.getStatus()),
                      "Trying to create an Account with other status than PENDING.");

        // Auto-accept if configured so
        final AccountSettings settings = accountSettingsService.retrieve();
        if (AccountSettings.AUTO_ACCEPT_MODE.equals(settings.getMode())) {
            accountWorkflowManager.acceptAccount(account);
        }

        // Create
        final Account newAccount = accountRepository.save(account);

        // Init the verification token
        tokenService.create(newAccount, pDto.getOriginUrl(), pDto.getRequestLink());
    }

    /**
     * Create the project user
     *
     * @param pDto
     * @throws EntityException
     */
    private void requestProjectUser(final AccessRequestDto pDto) throws EntityException {
        // Check that an associated account exists
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
        final ProjectUser projectUser = new ProjectUser(pDto.getEmail(), role, new ArrayList<>(), pDto.getMetadata());

        // Check the status
        Assert.isTrue(UserStatus.WAITING_ACCOUNT_ACTIVE.equals(projectUser.getStatus()),
                      "Trying to create a ProjectUser with other status than WAITING_ACCOUNT_ACTIVE.");

        // Save
        projectUserRepository.save(projectUser);
    }

}
