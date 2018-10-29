/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;
import fr.cnes.regards.modules.storage.service.IDataStorageService;

/**
 * REST controller handling request about monitoring of data storages
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(StorageMonitoringController.PATH)
public class StorageMonitoringController implements IResourceController<PluginStorageInfo> {

    /**
     * Controller base path
     */
    public static final String PATH = PrioritizedDataStorageController.BASE_PATH + "/monitoring";

    private static final String DIAGNOSTIC = "/diagnostic";

    /**
     * {@link IDataStorageService} instance
     */
    @Autowired
    private IDataStorageService dataStorageService;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve the monitoring information on the configured and active data storages
     * @return monitoring information on the configured and active data storages
     * @throws ModuleException
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of all active data storage monitoring information")
    public ResponseEntity<List<Resource<PluginStorageInfo>>> retrieveMonitoringInfos()
            throws ModuleException, IOException {
        return new ResponseEntity<>(toResources(dataStorageService.getMonitoringInfos()), HttpStatus.OK);
    }

    @RequestMapping(path = DIAGNOSTIC, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint allowing to get some diagnostic information on datastorages.")
    public ResponseEntity<List<Map<String, Object>>> getDiagnostic() {
        return new ResponseEntity<>(dataStorageService.getDiagnostics(), HttpStatus.OK);
    }

    @Override
    public Resource<PluginStorageInfo> toResource(PluginStorageInfo pElement, Object... pExtras) {
        Resource<PluginStorageInfo> resource = new Resource<>(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveMonitoringInfos", LinkRels.LIST);
        return resource;
    }
}
