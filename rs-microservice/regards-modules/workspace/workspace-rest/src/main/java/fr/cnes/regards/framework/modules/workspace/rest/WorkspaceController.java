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
package fr.cnes.regards.framework.modules.workspace.rest;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.modules.workspace.domain.WorkspaceMonitoringInformation;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * REST controller allowing to get workspace monitoring information.
 *
 * @author svissier
 */
@RestController
@ModuleInfo(name = "workspace-rest", version = "2.0.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(WorkspaceController.BASE_PATH)
public class WorkspaceController implements IResourceController<WorkspaceMonitoringInformation> {

    /**
     * the controller base path
     */
    public static final String BASE_PATH = "/workspaces";

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * {@link IWorkspaceService} instance
     */
    @Autowired
    private IWorkspaceService workspaceService;

    /**
     * @return workspace monitoring information wrapped into a {@link Resource}
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send monitoring informations about the workspace", role = DefaultRole.INSTANCE_ADMIN)
    public HttpEntity<Resource<WorkspaceMonitoringInformation>> getMonitoringInformation() throws IOException {
        return new ResponseEntity<>(toResource(workspaceService.getMonitoringInformation()), HttpStatus.OK);
    }

    @Override
    public Resource<WorkspaceMonitoringInformation> toResource(WorkspaceMonitoringInformation pElement,
            Object... pExtras) {
        Resource<WorkspaceMonitoringInformation> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "getMonitoringInformation", LinkRels.SELF);
        return resource;
    }
}
