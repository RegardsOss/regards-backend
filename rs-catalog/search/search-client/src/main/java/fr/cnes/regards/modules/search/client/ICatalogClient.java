package fr.cnes.regards.modules.search.client;

import java.util.Map;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;

/**
 * @author oroussel
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = ICatalogClient.PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ICatalogClient {

    String DATASET_PATH = "/datasets";

    String DATAOBJECTS_COMPUTE_FILES_SUMMARY = "/dataobjects/computefilessummary";

    /**
     * Return dataset
     * @param urn dataset IP_ID
     * @return the dataset
     */
    @RequestMapping(path = DATASET_PATH + "/{urn}", method = RequestMethod.GET)
    ResponseEntity<Resource<Dataset>> getDataset(@PathVariable("urn") final UniformResourceName urn);

    /**
     * The main path
     */
    String PATH = "";

    @RequestMapping(path = DATAOBJECTS_COMPUTE_FILES_SUMMARY, method = RequestMethod.GET)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "datasetIpId", required = false) final String datasetIpId,
            @RequestParam(value = "fileTypes") final String... fileTypes);
}
