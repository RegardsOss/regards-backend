/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.licence.LicenseService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class LicenseServiceTest {

    private IProjectsClient projectClient;

    private IProjectUserService projectUserService;

    private LicenseService licenseService;

    private Project projectWithLicense;

    private ProjectUser currentUser;

    private Project projectWithoutLicense;

    private JWTService jwtService = new JWTService();

    @Before
    public void init() throws EntityNotFoundException {

        jwtService.setSecret("123456789");

        projectClient = Mockito.mock(IProjectsClient.class);
        projectUserService = Mockito.mock(IProjectUserService.class);

        projectWithLicense = new Project("desc", "icon", true, "WITH_LICENSE");
        projectWithLicense.setLicenseLink("URL");
        projectWithoutLicense = new Project("desc", "icon", true, "WITHOUT_LICENSE");
        Mockito.when(projectClient.retrieveProject(projectWithLicense.getName()))
                .thenReturn(new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(projectWithLicense),
                        HttpStatus.OK));
        Mockito.when(projectClient.retrieveProject(projectWithoutLicense.getName()))
                .thenReturn(new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(projectWithoutLicense),
                        HttpStatus.OK));

        currentUser = new ProjectUser("", new Role(DefaultRole.PUBLIC.toString(), null), new ArrayList<>(),
                new ArrayList<>());

        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(currentUser);

        licenseService = new LicenseService(projectUserService, projectClient);
    }

    @Test
    public void testRetrieveState() throws EntityNotFoundException {
        jwtService.injectMockToken(projectWithLicense.getName(), currentUser.getRole().getName());
        LicenseDTO dto = licenseService.retrieveLicenseState(projectWithLicense.getName());
        Assert.assertEquals(projectWithLicense.getLicenceLink(), dto.getLicenceLink());
        Assert.assertEquals(currentUser.isLicenseAccepted(), dto.isAccepted());

        jwtService.injectMockToken(projectWithoutLicense.getName(), currentUser.getRole().getName());
        dto = licenseService.retrieveLicenseState(projectWithoutLicense.getName());
        Assert.assertEquals("", dto.getLicenceLink());
        Assert.assertEquals(true, dto.isAccepted());
    }

    @Test
    public void testAccept() throws EntityException {
        jwtService.injectMockToken(projectWithLicense.getName(), currentUser.getRole().getName());
        LicenseDTO dto = licenseService.acceptLicense(projectWithLicense.getName());
        Assert.assertEquals(projectWithLicense.getLicenceLink(), dto.getLicenceLink());
        Assert.assertEquals(true, currentUser.isLicenseAccepted());
    }

}
