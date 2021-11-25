package fr.cnes.regards.modules.feature.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationCollection;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.service.job.FeatureCreationJob;

/**
 *
 * @author Sébastien Binda
 */
public interface IFeatureCreationService extends IAbstractFeatureService<FeatureCreationRequest> {

    /**
     * Register creation requests in database for further processing from incoming request events
     */
    RequestInfo<String> registerRequests(List<FeatureCreationRequestEvent> events);

    /**
     * Create a list of {@link FeatureCreationRequest} from a list of {@link Feature} stored in a {@link FeatureCreationCollection}
     * and return a {@link RequestInfo} full of request ids and occured errors
     * @param collection {@link FeatureCreationCollection} it contain all {@link Feature} to handle
     * @return {@link RequestInfo}
     */
    RequestInfo<String> registerRequests(FeatureCreationCollection collection);

    /**
     * Process batch of requests during job
     * @return new feature created
     */
    Set<FeatureEntity> processRequests(Set<Long> requests, FeatureCreationJob featureCreationJob);

    /**
     * Handle successful creation process
     * @param requests successful requests
     */
    void handleSuccessfulCreation(Set<FeatureCreationRequest> requests);

    /**
     * Handle storage response errors from storage microservice
     * @param errorRequests errors requests
     */
    void handleStorageError(Collection<RequestResultInfoDTO> errorRequests);

    /**
     * Find all {@link FeatureCreationRequest}s
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @param page
     * @return {@link FeatureCreationRequest}s
     */
    Page<FeatureCreationRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page);

}