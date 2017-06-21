/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.licence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
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

    private static final Logger LOG = LoggerFactory.getLogger(LicenseService.class);

    private final IProjectUserService projectUserService;

    private final IProjectsClient projectsClient;

    public LicenseService(IProjectUserService pProjectUserService, IProjectsClient pProjectsClient) {
        super();
        projectUserService = pProjectUserService;
        projectsClient = pProjectsClient;
    }

    public LicenseDTO retrieveLicenseState(String pProjectName) throws EntityNotFoundException {
        Project project = retrieveProject(pProjectName);
        if(!SecurityUtils.getActualRole().equals(DefaultRole.INSTANCE_ADMIN.toString())) {
            ProjectUser pu = projectUserService.retrieveCurrentUser();
            if ((project.getLicenceLink() != null) && !project.getLicenceLink().isEmpty()) {
                return new LicenseDTO(pu.isLicenseAccepted(), project.getLicenceLink());
            }
            return new LicenseDTO(true, project.getLicenceLink());
        } else {
            return new LicenseDTO(true, project.getLicenceLink());
        }
    }

    /**
     * @param pProjectName
     * @return
     * @throws EntityNotFoundException
     */
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

    public LicenseDTO acceptLicense(String pProjectName) throws EntityException {
        ProjectUser pu = projectUserService.retrieveCurrentUser();
        pu.setLicenseAccepted(true);
        projectUserService.updateUser(pu.getId(), pu);
        return retrieveLicenseState(pProjectName);
    }

    public void resetLicence() {
        projectUserService.resetLicence();
    }

}
