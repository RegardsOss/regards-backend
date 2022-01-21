/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Class managing the workflow of a project user by applying the right transitions according to its state.<br>
 * Proxies the transition methods by instanciating the right state class (AccessGrantedState, AccessDeniedState...).
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
@Service
@MultitenantTransactional
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

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.workflow.projectuser.IProjectUserTransitions#makeProjectUserWaitForQualification(fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser)
     */
    @Override
    public void makeWaitForQualification(ProjectUser pProjectUser) throws EntityTransitionForbiddenException {
        projectUserStateProvider.createState(pProjectUser).makeWaitForQualification(pProjectUser);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.accessrights.workflow.projectuser.IProjectUserTransitions#verifiyEmail(fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken)
     */
    @Override
    public void verifyEmail(EmailVerificationToken pEmailVerificationToken) throws EntityException {
        projectUserStateProvider.createState(pEmailVerificationToken.getProjectUser())
                .verifyEmail(pEmailVerificationToken);
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
    public void grantAccess(final ProjectUser pProjectUser) throws EntityException {
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
