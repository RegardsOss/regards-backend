package fr.cnes.regards.modules.search.client;

import java.util.Collection;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.entities.domain.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.domain.ComplexSearchRequest;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedResources;

@RestClient(name = "rs-catalog")
@RequestMapping(value = IComplexSearchClient.TYPE_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IComplexSearchClient {

    static final String TYPE_MAPPING = "/complex/search";

    static final String SUMMARY_MAPPING = "/summary";

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.POST, value = IComplexSearchClient.SUMMARY_MAPPING)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(
            @RequestBody Collection<ComplexSearchRequest> complexSearchRequests);

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> search(
            @RequestBody Collection<ComplexSearchRequest> complexSearchRequests,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

}
