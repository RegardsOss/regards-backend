package fr.cnes.regards.modules.feature.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.FeatureCopyJob;

/**
 * Service to register/schedule/treat {@link FeatureCopyRequest}
 * @author Kevin Marchois
 *
 */
public interface IFeatureCopyService extends IAbstractFeatureService {

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
     * @param page
     * @return {@link FeatureCopyRequest}s
     */
    Page<FeatureCopyRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page);

}