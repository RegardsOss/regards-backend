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
package fr.cnes.regards.modules.acquisition.rest;

import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(MonitoringController.TYPE_PATH)
public class MonitoringController implements IResourceController<AcquisitionProcessingChainMonitor> {

    public static final String TYPE_PATH = "/chain-monitoring";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAcquisitionProcessingService processingService;

    /**
     * Build all {@link AcquisitionProcessingChainMonitor} from {@link AcquisitionProcessingChain}s matching given
     * criterion.
     *
     * @param mode    {@link AcquisitionProcessingChainMode} search criteria
     * @param running {@link Boolean} search criteria
     * @param label   {@link String} search criteria
     * @return page of {@link AcquisitionProcessingChainMonitor}s
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Search for acquisition processing chain summaries", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<AcquisitionProcessingChainMonitor>>> search(
        @RequestParam(name = "mode", required = false) AcquisitionProcessingChainMode mode,
        @RequestParam(name = "running", required = false) Boolean running,
        @RequestParam(name = "label", required = false) String label,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<AcquisitionProcessingChainMonitor> assembler) {
        Page<AcquisitionProcessingChainMonitor> results = processingService.buildAcquisitionProcessingChainSummaries(
            label,
            running,
            mode,
            pageable);
        return new ResponseEntity<>(toPagedResources(results, assembler), HttpStatus.OK);
    }

    @Override
    public EntityModel<AcquisitionProcessingChainMonitor> toResource(AcquisitionProcessingChainMonitor element,
                                                                     Object... pExtras) {

        EntityModel<AcquisitionProcessingChainMonitor> resource = resourceService.toResource(element);

        if ((element != null) && (element.getChain() != null)
            && !processingService.isDeletionPending(element.getChain())) {
            MethodParam<Long> idParam = MethodParamFactory.build(Long.class, element.getChain().getId());
            Class<AcquisitionProcessingChainController> clazz = AcquisitionProcessingChainController.class;
            resourceService.addLink(resource,
                                    clazz,
                                    "update",
                                    LinkRels.UPDATE,
                                    idParam,
                                    MethodParamFactory.build(AcquisitionProcessingChain.class));
            if (processingService.canBeStarted(element)) {
                resourceService.addLink(resource,
                                        clazz,
                                        "startManualChain",
                                        LinkRelation.of("start"),
                                        idParam,
                                        MethodParamFactory.build(Optional.class));
            }
            if (element.isActive()) {
                resourceService.addLink(resource, clazz, "stopChain", LinkRelation.of("stop"), idParam);
            }
            if (!element.getChain().isActive()) {
                resourceService.addLink(resource, clazz, "delete", LinkRels.DELETE, idParam);
            }
            resourceService.addLink(resource,
                                    clazz,
                                    "updateStateAndMode",
                                    LinkRelation.of("patch"),
                                    idParam,
                                    MethodParamFactory.build(UpdateAcquisitionProcessingChain.class));
        }
        return resource;
    }

}
