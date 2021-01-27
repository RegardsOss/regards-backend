/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.toponyms;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import fr.cnes.regards.modules.toponyms.service.ToponymsService;

/**
 * REST Controller for {@link ToponymDTO} entities
 *
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(ToponymsRestConfiguration.ROOT_MAPPING)
public class ToponymsController implements IResourceController<ToponymDTO> {

    private final static int MAX_SEARCH_RESULTS = 100;

    /**
     * Toponym service
     */
    @Autowired
    private ToponymsService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Endpoint to retrieve all toponyms with pagination
     *
     * @param pageable
     * @param assembler
     * @return {@link ToponymDTO}s
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve all toponyms with pagination", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<ToponymDTO>>> find(
            @SortDefault(sort = "label", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ToponymDTO> assembler) throws EntityNotFoundException {
        Page<ToponymDTO> toponyms = service.findAll(pageable);
        PagedModel<EntityModel<ToponymDTO>> resources = toPagedResources(toponyms, assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve one toponym by his identifier
     *
     * @param businessId
     * @return {@link ToponymDTO}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = ToponymsRestConfiguration.TOPONYM_ID, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve one toponym by his identifier", role = DefaultRole.PUBLIC)
    public ResponseEntity<EntityModel<ToponymDTO>> get(@PathVariable("businessId") String businessId)
            throws EntityNotFoundException {
        Optional<ToponymDTO> toponym = service.findOne(businessId);
        if (toponym.isPresent()) {
            return new ResponseEntity<>(toResource(toponym.get()), HttpStatus.OK);
        } else {
            throw new EntityNotFoundException(businessId, ToponymDTO.class);
        }
    }

    /**
     * Endpoint to search for toponyms. Geometries are not retrivied and list content is limited to 100 entities.
     *
     * @param partialLabel
     * @param locale
     * @return {@link ToponymDTO}s
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = ToponymsRestConfiguration.SEARCH, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(
            description = "Endpoint to search for toponyms. Geometries are not retrivied and list content is limited to 100 entities.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<List<EntityModel<ToponymDTO>>> search(@RequestParam(required = false) String partialLabel,
            @RequestParam(required = false) String locale) throws EntityNotFoundException {
        if ((partialLabel != null) && !partialLabel.isEmpty()) {
            List<ToponymDTO> toponymes;
            if (locale != null) {
                toponymes = service.search(partialLabel, locale, MAX_SEARCH_RESULTS);
            } else {
                toponymes = service.search(partialLabel, ToponymLocaleEnum.EN.getLocale(), MAX_SEARCH_RESULTS);
            }
            return new ResponseEntity<>(toResources(toponymes), HttpStatus.OK);
        } else {
            Page<ToponymDTO> page = service.findAll(PageRequest.of(0, MAX_SEARCH_RESULTS));
            if ((page != null) && (page.getContent() != null)) {
                return new ResponseEntity<>(toResources(page.getContent()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
    }

    @Override
    public EntityModel<ToponymDTO> toResource(ToponymDTO element, Object... extras) {
        return resourceService.toResource(element);
    }

}
