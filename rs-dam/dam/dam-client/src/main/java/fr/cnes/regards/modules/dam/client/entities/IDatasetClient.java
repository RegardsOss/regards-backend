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
package fr.cnes.regards.modules.dam.client.entities;

import java.util.Collection;
import java.util.Set;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 */
@RestClient(name = "rs-dam", contextId = "rs-dam.dataset.client")
@RequestMapping(value = IDatasetClient.DATASET_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IDatasetClient {

    String DATASET_PATH = "/datasets";

    String DATASET_ID_PATH = "/{dataset_id}";

    String DATASET_IP_ID_PATH = "/ipId/{dataset_ipId}";

    String DATASET_ID_ASSOCIATE_PATH = DATASET_ID_PATH + "/associate";

    String DATASET_ID_DISSOCIATE_PATH = DATASET_ID_PATH + "/dissociate";

    String DATASET_IPID_PATH_FILE = "/{dataset_ipId}/file";

    String ENTITY_ASSOCS_MAPPING = "{datasetUrn}/assocs";


    /**
     * Retrieve a page of datasets
     * @return a page of datasets
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<Dataset>>> retrieveDatasets(@RequestParam("page") int page,
            @RequestParam("size") int size);

    /**
     * Retrieve a dataset using its id
     */
    @RequestMapping(method = RequestMethod.GET, value = DATASET_ID_PATH)
    ResponseEntity<EntityModel<Dataset>> retrieveDataset(@PathVariable("dataset_id") Long datasetId);

    /**
     * Retrieve a dataset using its ip id
     */
    @RequestMapping(method = RequestMethod.GET, value = DATASET_IP_ID_PATH)
    ResponseEntity<EntityModel<Dataset>> retrieveDataset(@PathVariable("dataset_ipId") String datasetIpId);

    /**
     * Delete dataset
     */
    @RequestMapping(method = RequestMethod.DELETE, value = DATASET_ID_PATH)
    ResponseEntity<Void> deleteDataset(@PathVariable("dataset_id") Long datasetId);

    /**
     * Update dataset
     */
    @RequestMapping(method = RequestMethod.PUT, value = DATASET_ID_PATH)
    ResponseEntity<EntityModel<Dataset>> updateDataset(@PathVariable("dataset_id") Long datasetId,
            @RequestBody Dataset dataset);

    /**
     * Entry point to handle dissociation of {@link Dataset} specified by its id to other entities
     * @param datasetId {@link Dataset} id
     * @param toBeDissociated entity to dissociate
     * @return {@link Dataset} as a {@link EntityModel}
     */
    @RequestMapping(method = RequestMethod.PUT, value = DATASET_ID_DISSOCIATE_PATH)
    ResponseEntity<EntityModel<Dataset>> dissociateDataset(@PathVariable("dataset_id") Long datasetId,
            @RequestBody Set<OaisUniformResourceName> toBeDissociated);

    /**
     * Entry point to handle association of {@link Dataset} specified by its id to other entities
     * @param datasetId {@link Dataset} id
     * @param toBeAssociatedWith entities to be associated
     * @return {@link Dataset} as a {@link EntityModel}
     */
    @RequestMapping(method = RequestMethod.PUT, value = DATASET_ID_ASSOCIATE_PATH)
    ResponseEntity<EntityModel<Dataset>> associateDataset(@PathVariable("dataset_id") Long datasetId,
            @RequestBody Set<OaisUniformResourceName> toBeAssociatedWith);

    @RequestMapping(path = ENTITY_ASSOCS_MAPPING, method = RequestMethod.GET)
    public ResponseEntity<Collection<ModelAttrAssoc>> getModelAttrAssocsForDataInDataset(
            @RequestParam(name = "datasetUrn") UniformResourceName datasetUrn);
}
