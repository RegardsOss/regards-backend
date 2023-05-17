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
package fr.cnes.regards.modules.toponyms;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJson;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;
import fr.cnes.regards.modules.toponyms.domain.ToponymsRestConfiguration;
import fr.cnes.regards.modules.toponyms.service.ToponymsService;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for {@link ToponymDTO} entities
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(ToponymsRestConfiguration.ROOT_MAPPING)
public class ToponymsController implements IResourceController<ToponymDTO> {

    /**
     * Hypermedia resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Toponym service
     */
    @Autowired
    private ToponymsService service;

    /**
     * Maximum number of search results retrieved
     */
    private final static int MAX_SEARCH_RESULTS = 100;

    /**
     * Only visible toponyms are retrieved for global searches
     */
    private final static boolean DEFAULT_TOPONYM_VISIBILITY = true;

    /**
     * Endpoint to retrieve all toponyms with pagination. By default only visible toponyms are retrieved.
     *
     * @return {@link ToponymDTO}s
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve all toponyms with pagination", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<ToponymDTO>>> find(
        @SortDefault(sort = "label", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<ToponymDTO> assembler) throws EntityNotFoundException {
        Page<ToponymDTO> toponyms = service.findAllByVisibility(ToponymLocaleEnum.EN.getLocale(),
                                                                DEFAULT_TOPONYM_VISIBILITY,
                                                                pageable);
        PagedModel<EntityModel<ToponymDTO>> resources = toPagedResources(toponyms, assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve one toponym by its identifier
     *
     * @param businessId Unique identifier of toponym to search for
     * @param simplified True for simplified geometry (minimize size)
     * @return {@link ToponymDTO}
     */
    @RequestMapping(value = ToponymsRestConfiguration.TOPONYM_ID,
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve one toponym by his identifier", role = DefaultRole.PUBLIC)
    public ResponseEntity<EntityModel<ToponymDTO>> get(@PathVariable("businessId") String businessId,
                                                       @RequestParam(required = false) Boolean simplified)
        throws EntityNotFoundException {
        Optional<ToponymDTO> toponym;
        if (simplified == null) {
            toponym = service.findOne(businessId, false);
        } else {
            toponym = service.findOne(businessId, simplified);
        }
        // if entity was not found, throw exception
        if (!toponym.isPresent()) {
            throw new EntityNotFoundException(businessId, ToponymDTO.class);
        } else {
            return new ResponseEntity<>(toResource(toponym.get()), HttpStatus.OK);
        }
    }

    /**
     * Endpoint to search for toponyms. Geometries are not retrieved and list content is limited to 100 entities.
     * By default only visible toponyms are retrieved.
     *
     * @return {@link ToponymDTO}s
     */
    @RequestMapping(value = ToponymsRestConfiguration.SEARCH,
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to search for toponyms. Geometries are not retrieved and list content is limited to 100 entities.",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<List<EntityModel<ToponymDTO>>> search(@RequestParam(required = false) String partialLabel,
                                                                @RequestParam(required = false, defaultValue = "en")
                                                                String locale) throws EntityNotFoundException {
        if ((partialLabel != null) && !partialLabel.isEmpty()) {
            List<ToponymDTO> toponymes = service.search(partialLabel,
                                                        locale,
                                                        DEFAULT_TOPONYM_VISIBILITY,
                                                        MAX_SEARCH_RESULTS);
            return new ResponseEntity<>(toResources(toponymes), HttpStatus.OK);
        } else {
            Page<ToponymDTO> page = service.findAllByVisibility(locale,
                                                                DEFAULT_TOPONYM_VISIBILITY,
                                                                PageRequest.of(0, MAX_SEARCH_RESULTS));

            return new ResponseEntity<>(toResources(page.getContent()), HttpStatus.OK);
        }
    }

    /**
     * Add a toponym in the database. All the toponyms added through this path will not be visible and thus will not
     * be retrieved from the global search. They will also have an expiration date
     *
     * @param toponymGeoJson the object containing the feature in geojson format, the user and the project initiating the request
     * @return toponymDTO
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to add a toponym", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ToponymDTO>> createNotVisibleToponym(@RequestBody ToponymGeoJson toponymGeoJson)
        throws ModuleException, JsonProcessingException {
        ToponymDTO toponymDTO = this.service.generateNotVisibleToponym(toponymGeoJson.getFeature(),
                                                                       toponymGeoJson.getUser(),
                                                                       toponymGeoJson.getProject());
        return new ResponseEntity<>(toResource(toponymDTO), HttpStatus.CREATED);
    }

    @Override
    public EntityModel<ToponymDTO> toResource(ToponymDTO toponymDTO, Object... extras) {
        EntityModel<ToponymDTO> resource = resourceService.toResource(toponymDTO);
        resourceService.addLink(resource,
                                this.getClass(),
                                "get",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, toponymDTO.getBusinessId()),
                                MethodParamFactory.build(Boolean.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "createNotVisibleToponym",
                                LinkRels.CREATE,
                                MethodParamFactory.build(ToponymGeoJson.class));
        return resource;
    }
}
