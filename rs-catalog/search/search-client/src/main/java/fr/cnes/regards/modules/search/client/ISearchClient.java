package fr.cnes.regards.modules.search.client;

import javax.validation.Valid;
import java.util.Map;

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
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;

/**
 * @author oroussel
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = ISearchClient.PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ISearchClient {

    String PATH = "/search";

    String COLLECTIONS_URN = "/collections/{urn}";

    String DATAOBJECTS_URN = "/dataobjects/{urn}";

    String DOCUMENTS_URN = "/documents/{urn}";

    String DATASET_URN_PATH = "/datasets/{urn}";
    
    String ENTITY_GET_MAPPING = "/entities/{urn}";

    String DATAOBJECTS_COMPUTE_FILES_SUMMARY = "/dataobjects/computefilessummary";

    String DATAOBJECTS_SEARCH_WITHOUT_FACETS = "/dataobjects";

    /**
     * Return dataset
     * @param urn dataset IP_ID
     * @return the dataset
     */
    @RequestMapping(path = DATASET_URN_PATH, method = RequestMethod.GET)
    ResponseEntity<Resource<Dataset>> getDataset(@PathVariable("urn") UniformResourceName urn);

    @RequestMapping(path = DATAOBJECTS_URN, method = RequestMethod.GET)
    ResponseEntity<Resource<DataObject>> getDataobject(@Valid @PathVariable("urn") UniformResourceName urn);

    @RequestMapping(path = COLLECTIONS_URN, method = RequestMethod.GET)
    ResponseEntity<Resource<Collection>> getCollection(@Valid @PathVariable("urn") UniformResourceName urn);

    @RequestMapping(path = DOCUMENTS_URN, method = RequestMethod.GET)
    ResponseEntity<Resource<Document>> getDocument(@Valid @PathVariable("urn") UniformResourceName urn);

    @RequestMapping(path = ENTITY_GET_MAPPING + "/access", method = RequestMethod.GET)
    ResponseEntity<Boolean> hasAccess(@PathVariable("urn") UniformResourceName urn);

    @RequestMapping(path = DATAOBJECTS_COMPUTE_FILES_SUMMARY, method = RequestMethod.GET)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(@RequestParam Map<String, String> allParams,
            @RequestParam(value = "datasetIpId", required = false) String datasetIpId,
            @RequestParam(value = "fileTypes") String... fileTypes);

    @RequestMapping(path = DATAOBJECTS_SEARCH_WITHOUT_FACETS, method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<DataObject>>> searchDataobjects(@RequestParam Map<String, String> allParams,
            @RequestParam("page") int page, @RequestParam("size") int size);
}
