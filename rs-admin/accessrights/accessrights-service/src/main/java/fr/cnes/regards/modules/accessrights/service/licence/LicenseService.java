/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Service handling link between project's license and project user
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
@MultitenantTransactional
@EnableFeignClients(clients = IProjectsClient.class)
public class LicenseService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);

    /**
     * Project user service
     */
    private final IProjectUserService projectUserService;

    /**
     * Project client
     */
    private final IProjectsClient projectsClient;

    /**
     * Authentication resolver
     */
    private final IAuthenticationResolver authResolver;

    public LicenseService(IProjectUserService pProjectUserService, IProjectsClient pProjectsClient,
            IAuthenticationResolver authResolver) {
        super();
        projectUserService = pProjectUserService;
        projectsClient = pProjectsClient;
        this.authResolver = authResolver;
    }

    /**
     * Retrieve the license state for the given project and the current user
     * @param pProjectName
     * @return the license state
     * @throws EntityNotFoundException
     */
    public LicenseDTO retrieveLicenseState(String pProjectName) throws EntityNotFoundException {
        Project project = retrieveProject(pProjectName);
        if (authResolver.getRole().equals(DefaultRole.INSTANCE_ADMIN.toString())) {
            return new LicenseDTO(true, project.getLicenceLink());
        } else {
            ProjectUser pu = projectUserService.retrieveCurrentUser();
            if ((project.getLicenceLink() != null) && !project.getLicenceLink().isEmpty()) {
                return new LicenseDTO(pu.isLicenseAccepted(), project.getLicenceLink());
            }
            return new LicenseDTO(true, project.getLicenceLink());
        }
    }


    private Project retrieveProject(String pProjectName) throws EntityNotFoundException {
        FeignSecurityManager.asSystem();
        ResponseEntity<Resource<Project>> response = projectsClient.retrieveProject(pProjectName);
        FeignSecurityManager.reset();
        if (!HttpUtils.isSuccess(response.getStatusCode())) {
            LOG.info("Response from the project Client is : " + response.getStatusCode().value());
            throw new EntityNotFoundException(pProjectName, Project.class);
        }
        return response.getBody().getContent();
    }

    /**
     * Accept the license of the given project for the current user
     * @param pProjectName
     * @return accepted license state
     * @throws EntityException
     */
    public LicenseDTO acceptLicense(String pProjectName) throws EntityException {
        ProjectUser pu = projectUserService.retrieveCurrentUser();
        pu.setLicenseAccepted(true);
        projectUserService.updateUser(pu.getId(), pu);
        return retrieveLicenseState(pProjectName);
    }

    /**
     * Reset the license state for all users of the current project
     */
    public void resetLicence() {
        projectUserService.resetLicence();
    }

}
