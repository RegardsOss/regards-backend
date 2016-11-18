/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.module.rest.exception.ModuleAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleForbiddenTransitionException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Class managing the workflow of a project user by applying the right transitions according to its state.<br>
 * Proxies the transition methods by instanciating the right state class (AccessGrantedState, AccessDeniedState...).
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Service
@Transactional
public class ProjectUserWorkflowManager implements IProjectUserTransitions {

    /**
     * Factory class for creating the right state (i.e. implementation of the {@link IProjectUserTransitions}) according
     * to the project user status
     */
    private final ProjectUserStateFactory projectUserStateFactory;

    /**
     * CRUD repository handling {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Service handling {@link Role}s. Autowired by Spring.
     */
    private final IRoleService roleService;

    /**
     * @param pProjectUserStateFactory
     *            the state factory
     * @param pProjectUserRepository
     *            the project users repository
     * @param pRoleService
     *            the roles service
     */
    public ProjectUserWorkflowManager(final ProjectUserStateFactory pProjectUserStateFactory,
            final IProjectUserRepository pProjectUserRepository, final IRoleService pRoleService) {
        super();
        projectUserStateFactory = pProjectUserStateFactory;
        projectUserRepository = pProjectUserRepository;
        roleService = pRoleService;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#requestProjectAccess(fr.cnes.
     * regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void requestProjectAccess(final AccessRequestDTO pDto)
            throws ModuleForbiddenTransitionException, ModuleEntityNotFoundException, ModuleAlreadyExistsException {
        // Check existence
        if (!projectUserRepository.findOneByEmail(pDto.getEmail()).isPresent()) {
            throw new ModuleAlreadyExistsException("The email " + pDto.getEmail() + "is already in use.");
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
    public void qualifyAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        projectUserStateFactory.createState(pProjectUser).qualifyAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#inactiveAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void inactiveAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        projectUserStateFactory.createState(pProjectUser).inactiveAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#activeAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void activeAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        projectUserStateFactory.createState(pProjectUser).activeAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#denyAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void denyAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        projectUserStateFactory.createState(pProjectUser).denyAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#grantAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void grantAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        projectUserStateFactory.createState(pProjectUser).grantAccess(pProjectUser);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserTransitions#removeAccess(fr.cnes.regards.
     * modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void removeAccess(final ProjectUser pProjectUser) throws ModuleForbiddenTransitionException {
        projectUserStateFactory.createState(pProjectUser).removeAccess(pProjectUser);
    }
}
