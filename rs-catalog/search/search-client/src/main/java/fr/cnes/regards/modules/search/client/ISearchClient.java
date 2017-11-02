package fr.cnes.regards.modules.search.client;

import javax.validation.Valid;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;

/**
 * @author oroussel
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = ISearchClient.PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ISearchClient {
    String PATH = "/search";

    String DATASET_URN_PATH = "/datasets/{urn}";

    String DATAOBJECTS_COMPUTE_FILES_SUMMARY = "/dataobjects/computefilessummary";

    String DATAOBJECTS_SEARCH_WITHOUT_FACETS = "/dataobjects";

    String ENTITY_GET_MAPPING = "/entities/{urn}";

    /**
     * Return dataset
     * @param urn dataset IP_ID
     * @return the dataset
     */
    @RequestMapping(path = DATASET_URN_PATH, method = RequestMethod.GET)
    ResponseEntity<Resource<Dataset>> getDataset(@PathVariable("urn") final UniformResourceName urn);


    @RequestMapping(path = DATAOBJECTS_COMPUTE_FILES_SUMMARY, method = RequestMethod.GET)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "datasetIpId", required = false) final String datasetIpId,
            @RequestParam(value = "fileTypes") final String... fileTypes);

    @RequestMapping(path = DATAOBJECTS_SEARCH_WITHOUT_FACETS, method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<DataObject>>> searchDataobjects(
            @RequestParam final Map<String, String> allParams, final Pageable pPageable);

    /**
     * Unified entity retrieval endpoint
     * @param pUrn the entity URN
     * @return an entity
     */
    @RequestMapping(path = ENTITY_GET_MAPPING, method = RequestMethod.GET)
    @ResourceAccess(description = "Return the entity of passed URN.", role = DefaultRole.PUBLIC)
    <E extends AbstractEntity> ResponseEntity<Resource<E>> getEntity(
            @Valid @PathVariable("urn") final UniformResourceName pUrn);
}
