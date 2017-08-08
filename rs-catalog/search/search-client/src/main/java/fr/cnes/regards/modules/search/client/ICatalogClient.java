package fr.cnes.regards.modules.search.client;

import java.util.Map;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DocFilesSummary;

/**
 * @author oroussel
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = ICatalogClient.PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ICatalogClient {

    String DATAOBJECTS_COMPUTE_FILES_SUMMARY = "/dataobjects/computefilessummary";

    /**
     * The main path
     */
    String PATH = "";

    @RequestMapping(path = DATAOBJECTS_COMPUTE_FILES_SUMMARY, method = RequestMethod.GET)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "datasetIpId", required = false) final String datasetIpId,
            @RequestParam(value = "fileTypes") final String[] fileTypes) throws SearchException
}
