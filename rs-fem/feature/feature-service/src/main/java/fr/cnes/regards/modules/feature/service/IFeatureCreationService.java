package fr.cnes.regards.modules.feature.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationCollection;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.service.job.FeatureCreationJob;

public interface IFeatureCreationService extends IAbstractFeatureService {

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
     * Schedule a job to process a batch of requests<br/>
     * Inside this list there is only one occurence of {@link FeatureCreationRequest} per {@link Feature} id
     * @return number of scheduled requests (0 if no request was scheduled)
     */
    int scheduleRequests();

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
     * Find all {@link FeatureCreationRequest}s
     * @param page
     * @return {@link FeatureCreationRequest}s
     */
    Page<FeatureCreationRequest> findRequests(FeatureRequestSearchParameters searchParameters, Pageable page);
}