/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.projectuser;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

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
     * @param pProjectUserStateProvider
     *            the state provider
     */
    public ProjectUserWorkflowManager(final ProjectUserStateProvider pProjectUserStateProvider) {
        super();
        projectUserStateProvider = pProjectUserStateProvider;
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
            throws EntityException {
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
