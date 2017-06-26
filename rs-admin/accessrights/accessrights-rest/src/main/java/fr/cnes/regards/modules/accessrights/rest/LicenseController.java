/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.service.licence.LicenseService;

/**
 * REST Controller to handle links between project's license and project's user
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@RequestMapping(LicenseController.PATH_LICENSE)
public class LicenseController implements IResourceController<LicenseDTO> {

    public static final String PATH_LICENSE = "/license/{projectName}";

    private static final String PATH_RESET = "/reset";

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve if the current user has accepted the license of the project",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<LicenseDTO>> retrieveLicense(@PathVariable("projectName") String pProjectName)
            throws EntityNotFoundException {
        LicenseDTO licenseDto = licenseService.retrieveLicenseState(pProjectName);
        return new ResponseEntity<>(toResource(licenseDto, pProjectName), HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Allow current user to accept the license of the project", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<LicenseDTO>> acceptLicense(@PathVariable("projectName") String pProjectName)
            throws EntityException {
        LicenseDTO licenseDto = licenseService.acceptLicense(pProjectName);
        return new ResponseEntity<>(toResource(licenseDto, pProjectName), HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT, path = PATH_RESET)
    @ResourceAccess(
            description = "Allow admins to invalidate the license of the project for all the users of the project",
            role = DefaultRole.ADMIN)
    public ResponseEntity<Resource<LicenseDTO>> resetLicense(@PathVariable("projectName") String projectName)
            throws EntityException {
        //this endpoint is called from the instance administration interface so we have to force a tenant for this execution
        runtimeTenantResolver.forceTenant(projectName);
        licenseService.resetLicence();
        runtimeTenantResolver.clearTenant();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     *
     * @param pElement
     *            element to convert
     * @param pExtras
     *            Extra URL path parameters for links extra[0] has to be given and should be the projectName
     * @return
     */
    @Override
    public Resource<LicenseDTO> toResource(LicenseDTO pElement, Object... pExtras) {
        Resource<LicenseDTO> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveLicense", LinkRels.SELF,
                                MethodParamFactory.build(String.class,(String) pExtras[0]));
        resourceService.addLink(resource, this.getClass(), "acceptLicense", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class,(String) pExtras[0]));
        resourceService.addLink(resource, this.getClass(), "resetLicense", LinkRels.DELETE,
                                MethodParamFactory.build(String.class,(String) pExtras[0]));
        return resource;
    }

}
