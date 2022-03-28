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
package fr.cnes.regards.modules.accessrights.service.licence;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service handling link between project's license and project user
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@MultitenantTransactional
public class LicenseService implements ILicenseService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);

    /**
     * Project user service
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Project client
     */
    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IPublisher publisher;

    /**
     * Retrieve the license state for the given project and the current user
     *
     * @return the license state
     * @throws EntityNotFoundException
     */
    @Override
    public LicenseDTO retrieveLicenseState() throws EntityNotFoundException {
        String licenceLink = retrieveProject(runtimeTenantResolver.getTenant()).getLicenceLink();
        boolean isLicenseAccepted = isInstanceAdmin() || noLicense(licenceLink) || isLicenseAcceptedByUser();
        return new LicenseDTO(isLicenseAccepted, licenceLink);
    }

    private Project retrieveProject(String pProjectName) throws EntityNotFoundException {
        FeignSecurityManager.asSystem();
        ResponseEntity<EntityModel<Project>> response = projectsClient.retrieveProject(pProjectName);
        FeignSecurityManager.reset();
        if (!HttpUtils.isSuccess(response.getStatusCode())) {
            LOG.info("Response from the project Client is : " + response.getStatusCode().value());
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return response.getBody().getContent();
    }

    private boolean isInstanceAdmin() {
        return authResolver.getRole().equals(DefaultRole.INSTANCE_ADMIN.toString());
    }

    private boolean noLicense(String licenceLink) {
        return licenceLink == null || licenceLink.isEmpty();
    }

    private boolean isLicenseAcceptedByUser() throws EntityNotFoundException {
        return projectUserService.retrieveCurrentUser().isLicenseAccepted();
    }

    /**
     * Accept the license of the given project for the current user
     *
     * @return accepted license state
     * @throws EntityException if
     */
    @Override
    public LicenseDTO acceptLicense() throws EntityException {
        ProjectUser projectUser = projectUserService.retrieveCurrentUser();
        projectUser.setLicenseAccepted(true);
        projectUserService.updateUser(projectUser.getId(), projectUser);
        LicenseDTO license = retrieveLicenseState();
        publisher.publish(new LicenseEvent(LicenseAction.ACCEPT, projectUser.getEmail(), license.getLicenceLink()));
        return license;
    }

    /**
     * Reset the license state for all users of the current project
     */
    @Override
    public void resetLicence() {
        projectUserService.resetLicence();
        publisher.publish(new LicenseEvent(LicenseAction.RESET, "", ""));
    }
}
