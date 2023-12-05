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
package fr.cnes.regards.modules.dam.client.dataaccess;

import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.AccessRight;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RestClient(name = "rs-dam", contextId = "rs-dam.access-right.client")
public interface IAccessRightClient { // NOSONAR

    /**
     * Client base path
     */
    String ROOT_PATH_ACCESS_RIGHTS = "/accessrights";

    String PATH_ACCESS_RIGHT = "/accessright";

    /**
     * Client path using an access right id as path variable
     */
    String PATH_ACCESS_RIGHTS_ID = "/{accessright_id}";

    /**
     * Client path to know if a dataset is accessible
     */
    String PATH_IS_DATASET_ACCESSIBLE = "/isAccessible";

    /**
     * Retrieve access rights
     *
     * @param groupName   group which the access right should be applied to
     * @param datasetIpId dataset id which the access right should be applied to
     * @return the retrieved access rights
     */
    @GetMapping(path = ROOT_PATH_ACCESS_RIGHTS,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<PagedModel<EntityModel<AccessRight>>> retrieveAccessRightsList(
        @RequestParam(name = "accessgroup", required = false) String groupName,
        @RequestParam(name = "dataset", required = false) OaisUniformResourceName datasetIpId,
        @RequestParam("page") int page,
        @RequestParam("size") int size);

    @GetMapping(path = ROOT_PATH_ACCESS_RIGHTS + PATH_ACCESS_RIGHT,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<AccessRight> retrieveAccessRight(@RequestParam(name = "accessgroup") String accessGroupName,
                                                    @RequestParam(name = "dataset")
                                                    OaisUniformResourceName datasetIpId);

    /**
     * Create an access right
     *
     * @return created access right
     */
    @PostMapping(path = ROOT_PATH_ACCESS_RIGHTS,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<EntityModel<AccessRight>> createAccessRight(@Valid @RequestBody AccessRight accessRight);

    /**
     * Retrieve an access right by its id
     *
     * @return retrieved access right
     */
    @GetMapping(path = ROOT_PATH_ACCESS_RIGHTS + PATH_ACCESS_RIGHTS_ID,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<EntityModel<AccessRight>> retrieveAccessRight(@Valid @PathVariable("accessright_id") Long id);

    /**
     * Update an access right. pToBe id should be the same as pId
     *
     * @return updated access right
     */
    @PutMapping(path = ROOT_PATH_ACCESS_RIGHTS + PATH_ACCESS_RIGHTS_ID,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<EntityModel<AccessRight>> updateAccessRight(@Valid @PathVariable("accessright_id") Long id,
                                                               @Valid AccessRight toBe);

    /**
     * Delete access right by its id
     */
    @DeleteMapping(path = ROOT_PATH_ACCESS_RIGHTS + PATH_ACCESS_RIGHTS_ID,
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> deleteAccessRight(@Valid @PathVariable("accessright_id") Long id);

    @GetMapping(path = ROOT_PATH_ACCESS_RIGHTS + PATH_IS_DATASET_ACCESSIBLE,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Boolean> isUserAutorisedToAccessDataset(
        @RequestParam(name = "dataset") OaisUniformResourceName datasetIpId,
        @RequestParam(name = "user") String userEMail);
}
