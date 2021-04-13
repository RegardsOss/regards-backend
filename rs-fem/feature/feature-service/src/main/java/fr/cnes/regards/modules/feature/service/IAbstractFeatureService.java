package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.amqp.event.IRequestValidation;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAbstractFeatureService extends IRequestDeniedService, IRequestValidation {

    /**
     * Schedule a job to process a batch of requests<br/>
     * @return number of scheduled requests (0 if no request was scheduled)
     */
    int scheduleRequests();

    /**
     * Find requests information with search parameters context
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestsInfo}
     */
    RequestsInfo getInfo(FeatureRequestsSelectionDTO selection);

    /**
     * Delete requests associated to given search parameters.
     * Number of requests deletable is limited as this method is synchronous. Number of handled requests is returned in response.
     *
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     */
    RequestHandledResponse deleteRequests(FeatureRequestsSelectionDTO selection);

    /**
     * Retry requests associated to given search parameters
     * Number of requests retryable is limited as this method is synchronous. Number of handled requests is returned in response.
     *
     * @param selection {@link FeatureRequestsSelectionDTO}
     * @return {@link RequestHandledResponse}
     */
    RequestHandledResponse retryRequests(FeatureRequestsSelectionDTO selection);

}
