package fr.cnes.regards.modules.feature.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
public class RequestRetryService implements IRequestRetryService {

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepository;

    //TODO according to filter parameters, we may need to schedule a job to search request to be retried

    /**
     * retry of request is simply done by setting the right step to requests then, schedulers will take them into account
     */
    @Override
    public void retryRequest(Set<AbstractFeatureRequest> requestsToRetry) {
        //first lets decide which step should be retried: process or notification
        Set<Long> notificationsToRetry = new HashSet<>();
        Set<Long> processToRetry = new HashSet<>();
        for (AbstractFeatureRequest request : requestsToRetry) {
            //only retry requests in error
            if (request.getState() == RequestState.ERROR) {
                if (request.getStep() == FeatureRequestStep.REMOTE_NOTIFICATION_ERROR) {
                    notificationsToRetry.add(request.getId());
                } else {
                    processToRetry.add(request.getId());
                }
            }
        }
        abstractFeatureRequestRepository.updateStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, notificationsToRetry);
        abstractFeatureRequestRepository.updateStep(FeatureRequestStep.LOCAL_DELAYED, processToRetry);
    }

}
