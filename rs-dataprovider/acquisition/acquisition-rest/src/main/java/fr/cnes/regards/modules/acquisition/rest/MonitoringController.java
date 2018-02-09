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
package fr.cnes.regards.modules.acquisition.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

@RestController
@ModuleInfo(name = "Acqusition processing chain monitoring", description = "Acqusition processing chain monitoring",
        version = "2.0.0-SNAPSHOT", author = "CSSI", legalOwner = "CNES",
        documentation = "https://github.com/RegardsOss")
@RequestMapping(MonitoringController.TYPE_PATH)
public class MonitoringController implements IResourceController<AcquisitionProcessingChainMonitor> {

    public static final String TYPE_PATH = "/chain-monitoring";

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAcquisitionProcessingService service;

    /**
     * Build all {@link AcquisitionProcessingChainMonitor} from {@link AcquisitionProcessingChain}s matching given
     * criterion.
     * @param mode {@link AcquisitionProcessingChainMode} search criteria
     * @param running {@link Boolean} search criteria
     * @param label {@link String} search criteria
     * @param pageable
     * @param assembler
     * @return page of {@link AcquisitionProcessingChainMonitor}s
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Search for acquisition processing chain summaries", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<AcquisitionProcessingChainMonitor>>> search(
            @RequestParam(name = "mode", required = false) AcquisitionProcessingChainMode mode,
            @RequestParam(name = "running", required = false) Boolean running,
            @RequestParam(name = "label", required = false) String label, Pageable pageable,
            PagedResourcesAssembler<AcquisitionProcessingChainMonitor> assembler) throws ModuleException {
        Page<AcquisitionProcessingChainMonitor> results = service
                .buildAcquisitionProcessingChainSummaries(label, running, mode, pageable);
        return new ResponseEntity<>(toPagedResources(results, assembler), HttpStatus.OK);
    }

    @Override
    public Resource<AcquisitionProcessingChainMonitor> toResource(AcquisitionProcessingChainMonitor element,
            Object... pExtras) {
        Resource<AcquisitionProcessingChainMonitor> resource = resourceService.toResource(element);
        if ((element != null) && (element.getChain() != null)) {
            if (AcquisitionProcessingChainMode.MANUAL.equals(element.getChain().getMode())) {
                resourceService.addLink(resource, AcquisitionProcessingChainController.class, "startManualChain",
                                        "start", MethodParamFactory.build(Long.class, element.getChain().getId()));
            }
            if (element.isActive()) {
                resourceService.addLink(resource, AcquisitionProcessingChainController.class, "stopChain", "stop",
                                        MethodParamFactory.build(Long.class, element.getChain().getId()));
            }
        }
        return resource;
    }

}
