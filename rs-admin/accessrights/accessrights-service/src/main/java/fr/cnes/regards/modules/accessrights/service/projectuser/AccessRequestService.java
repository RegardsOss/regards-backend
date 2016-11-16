/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * {@link IAccessRequestService} implementation
 *
 * @author CS SI
 */
@Service
public class AccessRequestService implements IAccessRequestService {

    /**
     * CRUD repository managing project users. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service handling CRUD operation on {@link Account}s
     */
    private final IAccountService accountService;

    /**
     * Service handling CRUD operation on {@link Role}s
     */
    private final IRoleService roleService;

    /**
     * Creates an {@link AccessRequestService} wired to the given {@link IProjectUserRepository}.
     *
     * @param pProjectUserRepository
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pAccountService
     *            Autowired by Spring. Must not be {@literal null}.
     * @param pRoleService
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public AccessRequestService(final IProjectUserRepository pProjectUserRepository,
            final IAccountService pAccountService, final IRoleService pRoleService) {
        super();
        projectUserRepository = pProjectUserRepository;
        accountService = pAccountService;
        roleService = pRoleService;
    }

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        return projectUserRepository.findByStatus(UserStatus.WAITING_ACCESS);
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
            final Account account = new Account(pDto.getEmail(), pDto.getFirstName(), pDto.getLastName(),
                    pDto.getPassword());

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

        // Save it
        projectUserRepository.save(projectUser);
        return pDto;
    }

    @Override
    public void removeAccessRequest(final Long pAccessId) throws ModuleEntityNotFoundException {
        if (exists(pAccessId)) {
            projectUserRepository.delete(pAccessId);
        } else {
            throw new ModuleEntityNotFoundException(pAccessId.toString(), ProjectUser.class);
        }
    }

    @Override
    public void acceptAccessRequest(final Long pAccessId) throws ModuleEntityNotFoundException {
        final ProjectUser projectUser = findById(pAccessId);
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        projectUserRepository.save(projectUser);
    }

    @Override
    public void denyAccessRequest(final Long pAccessId) throws ModuleEntityNotFoundException {
        final ProjectUser projectUser = findById(pAccessId);
        projectUser.setStatus(UserStatus.ACCESS_DENIED);
        projectUserRepository.save(projectUser);
    }

    @Override
    public boolean exists(final Long pId) {
        try (Stream<ProjectUser> stream = retrieveAccessRequestList().stream()) {
            return stream.filter(p -> p.getId().equals(pId)).findFirst().isPresent();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.role.IAccessRequestService#existsByEmail(java.lang.String)
     */
    @Override
    public boolean existsByEmail(final String pEmail) {
        try (Stream<ProjectUser> stream = retrieveAccessRequestList().stream()) {
            return stream.filter(p -> p.getEmail().equals(pEmail)).findFirst().isPresent();
        }
    }

    /**
     * Retrieves the unique access request, that is to say the {@link ProjectUser} with <code>status</code> equal to
     * {@link UserStatus#WAITING_ACCESS}, with specified <code>id</code>.
     *
     * @param pId
     *            The access request <code>id</code>
     * @return The found access request
     * @throws ModuleEntityNotFoundException
     *             Thrown when a project user with passed id could not be found
     */
    private ProjectUser findById(final Long pId) throws ModuleEntityNotFoundException {
        try (Stream<ProjectUser> stream = retrieveAccessRequestList().stream()) {
            return stream.filter(p -> p.getId() == pId).findFirst()
                    .orElseThrow(() -> new ModuleEntityNotFoundException(pId.toString(), ProjectUser.class));
        }
    }

}
