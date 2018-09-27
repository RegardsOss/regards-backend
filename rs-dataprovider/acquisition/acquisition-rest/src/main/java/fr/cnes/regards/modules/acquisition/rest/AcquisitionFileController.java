/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;

@RestController
@RequestMapping(AcquisitionFileController.TYPE_PATH)
public class AcquisitionFileController implements IResourceController<AcquisitionFile> {

    public static final String TYPE_PATH = "/acquisition-files";

    public static final String REQUEST_PARAM_FILEPATH = "filePath";

    public static final String REQUEST_PARAM_STATE = "state";

    public static final String REQUEST_PARAM_PRODUCT_ID = "productId";

    public static final String REQUEST_PARAM_CHAIN_ID = "chainId";

    public static final String REQUEST_PARAM_FROM = "from";

    @Autowired
    private IAcquisitionFileService fileService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Search for {@link AcquisitionFile} entities matching parameters
     * @param filePath {@link String}
     * @param state {@link AcquisitionFileState}
     * @param productId {@link Long} identifier of {@link Product}
     * @param from {@link OffsetDateTime}
     * @return {@link AcquisitionFile}s
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Search for acquisition files", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedResources<Resource<AcquisitionFile>>> search(
            @RequestParam(name = REQUEST_PARAM_FILEPATH, required = false) String filePath,
            @RequestParam(name = REQUEST_PARAM_STATE, required = false) List<AcquisitionFileState> state,
            @RequestParam(name = REQUEST_PARAM_PRODUCT_ID, required = false) Long productId,
            @RequestParam(name = REQUEST_PARAM_CHAIN_ID, required = false) Long chainId,
            @RequestParam(name = REQUEST_PARAM_FROM, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AcquisitionFile> assembler) {
        Page<AcquisitionFile> files = fileService.search(filePath, state, productId, chainId, from, pageable);
        return new ResponseEntity<>(toPagedResources(files, assembler), HttpStatus.OK);
    }

    @Override
    public Resource<AcquisitionFile> toResource(AcquisitionFile element, Object... extras) {
        return resourceService.toResource(element);
    }
}
