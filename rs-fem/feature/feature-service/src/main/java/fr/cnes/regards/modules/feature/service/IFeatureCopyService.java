package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.FeatureCopyJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service to register/schedule/treat {@link FeatureCopyRequest}
 *
 * @author Kevin Marchois
 */
public interface IFeatureCopyService extends IAbstractFeatureService<FeatureCopyRequest> {

    /**
     * Register copy requests in database for further processing from incoming request events
     */
    RequestInfo<FeatureUniformResourceName> registerRequests(List<FeatureCopyRequest> requests);

    /**
     * Process batch of requests during job
     */
    void processRequests(List<FeatureCopyRequest> requests, FeatureCopyJob featureCopyJob);

    /**
     * Find all {@link FeatureCopyRequest}s
     *
     * @return {@link FeatureCopyRequest}s
     */
    Page<FeatureCopyRequest> findRequests(SearchFeatureRequestParameters filters, Pageable page);

    /**
     * Find requests information
     *
     * @param filters {@link SearchFeatureRequestParameters}
     * @return {@link RequestsInfo}
     */
    RequestsInfo getInfo(SearchFeatureRequestParameters filters);

}