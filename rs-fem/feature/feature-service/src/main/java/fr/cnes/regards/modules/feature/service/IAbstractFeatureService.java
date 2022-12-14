package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.amqp.event.IRequestDeniedService;
import fr.cnes.regards.framework.amqp.event.IRequestValidation;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

import java.util.Collection;
import java.util.Map;

/**
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public interface IAbstractFeatureService<R extends AbstractFeatureRequest>
    extends IRequestDeniedService, IRequestValidation {

    /**
     * Schedule a job to process a batch of requests<br/>
     *
     * @return number of scheduled requests (0 if no request was scheduled)
     */
    int scheduleRequests();

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

    Map<FeatureUniformResourceName, ILightFeatureEntity> getSessionInfoByUrn(Collection<FeatureUniformResourceName> uniformResourceNames);

    void doOnSuccess(Collection<R> requests);

    void doOnTerminated(Collection<R> requests);

    void doOnError(Collection<R> requests);

}
