/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.projectuser;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Class managing the workflow of a project user by applying the right transitions according to its state.<br>
 * Proxies the transition methods by instanciating the right state class (AccessGrantedState, AccessDeniedState...).
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Service
@Primary
public class ProjectUserWorkflowManager implements IProjectUserTransitions {

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final ProjectUserStateProvider projectUserStateProvider;

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service handling {@link Role}s. Autowired by Spring.
     */
    private final IRoleService roleService;

    /**
     * Service handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountService accountService;

    /**
     * @param pProjectUserStateProvider
     *            the state provider
     * @param pProjectUserRepository
     *            the project users repository
     * @param pRoleService
     *            the roles service
     * @param pAccountService
     *            the accounts service
     */
    public ProjectUserWorkflowManager(final ProjectUserStateProvider pProjectUserStateProvider,
            final IProjectUserRepository pProjectUserRepository, final IRoleService pRoleService,
            final IAccountService pAccountService) {
        super();
        projectUserStateProvider = pProjectUserStateProvider;
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
        accountService = pAccountService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#requestProjectAccess(fr.cnes.
     * regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void requestProjectAccess(final AccessRequestDto pDto) throws EntityException {
        // Check that an associated account exitsts
        if (!accountService.existAccount(pDto.getEmail())) {
            throw new EntityNotFoundException(pDto.getEmail(), Account.class);
        }

        // Check that no project user with same email exists
        if (projectUserRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new EntityAlreadyExistsException("The email " + pDto.getEmail() + "is already in use.");
        }

        // Retrieve the role - if null, use default role
        final Optional<String> roleName = Optional.ofNullable(pDto.getRoleName());
        final Role role;
        if (roleName.isPresent()) {
            role = roleService.retrieveRole(roleName.get());
        } else {
            role = roleService.getDefaultRole();
        }

        // Create a new project user
        final ProjectUser projectUser = new ProjectUser(pDto.getEmail(), role, pDto.getPermissions(),
                pDto.getMetaData());

        // Check the status
        Assert.isTrue(UserStatus.WAITING_ACCESS.equals(projectUser.getStatus()),
                      "Trying to create a ProjectUser with other status than WAITING_ACCESS.");

        // Save
        projectUserRepository.save(projectUser);
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
            throws EntityTransitionForbiddenException, EntityNotFoundException {
        projectUserStateProvider.createState(pProjectUser).qualifyAccess(pProjectUser, pQualification);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#inactiveAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void inactiveAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        projectUserStateProvider.createState(pProjectUser).inactiveAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#activeAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void activeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        projectUserStateProvider.createState(pProjectUser).activeAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#denyAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void denyAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        projectUserStateProvider.createState(pProjectUser).denyAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#grantAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void grantAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        projectUserStateProvider.createState(pProjectUser).grantAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#removeAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void removeAccess(final ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        projectUserStateProvider.createState(pProjectUser).removeAccess(pProjectUser);
    }
}
