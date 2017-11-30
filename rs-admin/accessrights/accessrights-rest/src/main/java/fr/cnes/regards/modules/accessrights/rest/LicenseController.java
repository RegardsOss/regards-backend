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

    /**
     * Controller base path
     */
    public static final String PATH_LICENSE = "/license/{projectName}";

    /**
     * Controller path to reset the license
     */
    private static final String PATH_RESET = "/reset";

    /**
     * {@link LicenseService} instance
     */
    @Autowired
    private LicenseService licenseService;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve if the current user has accepted the license of the given project, represented by its name.
     * @param pProjectName
     * @return if the current user has accepted the license of the project
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve if the current user has accepted the license of the project",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<LicenseDTO>> retrieveLicense(@PathVariable("projectName") String pProjectName)
            throws EntityNotFoundException {
        LicenseDTO licenseDto = licenseService.retrieveLicenseState(pProjectName);
        return new ResponseEntity<>(toResource(licenseDto, pProjectName), HttpStatus.OK);
    }

    /**
     * Accept the license for the current user for the given project, represented by its name
     * @param pProjectName
     * @return the license state
     * @throws EntityException
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "Allow current user to accept the license of the project", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<LicenseDTO>> acceptLicense(@PathVariable("projectName") String pProjectName)
            throws EntityException {
        LicenseDTO licenseDto = licenseService.acceptLicense(pProjectName);
        return new ResponseEntity<>(toResource(licenseDto, pProjectName), HttpStatus.OK);
    }

    /**
     * Reset the license for the given project, represented by its name.
     * @param projectName
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT, path = PATH_RESET)
    @ResourceAccess(
            description = "Allow admins to invalidate the license of the project for all the users of the project",
            role = DefaultRole.ADMIN)
    public ResponseEntity<Void> resetLicense(@PathVariable("projectName") String projectName) {
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
