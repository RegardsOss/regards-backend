/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.IAccessRequestService;
import fr.cnes.regards.modules.accessrights.service.IAccountService;
import fr.cnes.regards.modules.accessrights.service.IRoleService;
import fr.cnes.regards.modules.accessrights.service.RoleService;

/**
 * Stubbed {@link IAccessRequestService} implementation
 *
 * @author CS SI
 */
@Service
@Profile("test")
@Primary
public class AccessRequestServiceStub implements IAccessRequestService {

    /**
     * The stub project users data base
     */
    private List<ProjectUser> projectUsers = new ArrayList<>();

    /**
     * Service handling CRUD operation on {@link Account}s
     */
    private final IAccountService accountService;

    /**
     * Service handling CRUD operation on {@link Role}s
     */
    private final IRoleService roleService;

    /**
     * Create a new stub implementation of {@link IAccessRequestService} with passed repositories
     *
     * @param pAccountService
     *            The account repository
     * @param pRoleRepository
     *            The role repository
     */
    public AccessRequestServiceStub(final IAccountService pAccountService, final IRoleRepository pRoleRepository) {
        accountService = pAccountService;
        roleService = new RoleService(pRoleRepository);

        String login = "login0@test.com";
        final ProjectUser projectUser0 = new ProjectUser();
        projectUser0.setId(0L);
        projectUser0.setEmail(login);
        projectUser0.setStatus(UserStatus.WAITING_ACCESS);
        projectUsers.add(projectUser0);

        login = "login1@test.com";
        final ProjectUser projectUser1 = new ProjectUser();
        projectUser1.setId(1L);
        projectUser1.setEmail(login);
        projectUser1.setStatus(UserStatus.ACCESS_GRANTED);
        projectUsers.add(projectUser1);

        login = "login2@test.com";
        final ProjectUser projectUser2 = new ProjectUser();
        projectUser2.setId(2L);
        projectUser2.setEmail(login);
        projectUser2.setStatus(UserStatus.ACCESS_DENIED);
        projectUsers.add(projectUser2);

    }

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .collect(Collectors.toList());
    }

    @Override
    public AccessRequestDTO requestAccess(final AccessRequestDTO pDto) throws AlreadyExistingException {
        if (existsByEmail(pDto.getEmail())) {
            throw new AlreadyExistingException(
                    pDto.getEmail() + " already has made an access request for this project");
        }

        // If no role provided, set to default role
        if (pDto.getRole() == null) {
            pDto.setRole(roleService.getDefaultRole());
        }

        // If no associated account
        if (!accountService.existAccount(pDto.getEmail())) {
            // Initialize a new final Account with provided final info and the final pending status
            final Account account = new Account();
            account.setEmail(pDto.getEmail());
            account.setFirstName(pDto.getFirstName());
            account.setLastName(pDto.getLastName());
            account.setLogin(pDto.getLogin());
            account.setPassword(pDto.getPassword());
            account.setStatus(AccountStatus.PENDING);

            // Create it via the account service
            accountService.createAccount(account);
        }

        // Initialize the project user
        final ProjectUser projectUser = new ProjectUser();
        projectUser.setEmail(pDto.getEmail());
        projectUser.setPermissions(pDto.getPermissions());
        projectUser.setRole(pDto.getRole());
        projectUser.setMetaData(pDto.getMetaData());
        projectUser.setStatus(UserStatus.WAITING_ACCESS);

        // "Save" it
        projectUsers.add(projectUser);
        return pDto;

    }

    /**
     * Check thats the request access (project user) with passed <code>email</code> exists
     *
     * @param pEmail
     *            The email
     * @return <code>True</code> if exists, esle <code>False</code>
     */
    public boolean existAccessRequest(final String pEmail) {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .filter(p -> p.getEmail().equals(pEmail)).findFirst().isPresent();
    }

    @Override
    public boolean exists(final Long pId) {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .filter(p -> p.getId() == pId).findFirst().isPresent();
    }

    @Override
    public void removeAccessRequest(final Long pAccessId) throws ModuleEntityNotFoundException {
        if (exists(pAccessId)) {
            projectUsers = projectUsers.stream().filter(p -> p.getId() != pAccessId).collect(Collectors.toList());
            return;
        }
        throw new ModuleEntityNotFoundException(pAccessId.toString(), ProjectUser.class);
    }

    @Override
    public void acceptAccessRequest(final Long pAccessId) throws ModuleEntityNotFoundException {
        if (!exists(pAccessId)) {
            throw new ModuleEntityNotFoundException(pAccessId.toString(), ProjectUser.class);
        }

        for (final ProjectUser projectUser : projectUsers) {
            if (projectUser.getId() == pAccessId) {
                projectUser.accept();
            }
        }
    }

    @Override
    public void denyAccessRequest(final Long pAccessId) throws ModuleEntityNotFoundException {
        if (!exists(pAccessId)) {
            throw new ModuleEntityNotFoundException(pAccessId.toString(), ProjectUser.class);
        }

        for (final ProjectUser projectUser : projectUsers) {
            if (projectUser.getId() == pAccessId) {
                projectUser.deny();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IAccessRequestService#existsByEmail(java.lang.String)
     */
    @Override
    public boolean existsByEmail(final String pEmail) {
        return projectUsers.stream().filter(p -> p.getStatus().equals(UserStatus.WAITING_ACCESS))
                .filter(p -> p.getEmail().equals(pEmail)).findFirst().isPresent();
    }

}
