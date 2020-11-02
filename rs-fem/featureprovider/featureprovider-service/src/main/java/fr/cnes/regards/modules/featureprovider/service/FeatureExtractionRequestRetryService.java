package fr.cnes.regards.modules.featureprovider.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureExtractionRequestRepository;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
public class FeatureExtractionRequestRetryService implements IFeatureExtractionRequestRetryService {

    @Autowired
    private IFeatureExtractionRequestRepository featureExtractionRequestRepository;

    //TODO according to filter parameters, we may need to schedule a job to search request to be retried
    /**
     * retry of request is simply done by setting the right step to requests then, schedulers will take them into account
     */
    @Override
    public void retryRequest(Set<FeatureExtractionRequest> requestsToRetry) {
        //first lets decide which step should be retried: process or notification
        Set<Long> notificationsToRetry = new HashSet<>();
        Set<Long> processToRetry = new HashSet<>();
        for (FeatureExtractionRequest request : requestsToRetry) {
            //only retry requests in error
            if (request.getState() == RequestState.ERROR) {
                if (request.getStep() == FeatureRequestStep.REMOTE_NOTIFICATION_ERROR) {
                    notificationsToRetry.add(request.getId());
                } else {
                    processToRetry.add(request.getId());
                }
            }
        }
        featureExtractionRequestRepository.updateStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, notificationsToRetry);
        featureExtractionRequestRepository.updateStep(FeatureRequestStep.LOCAL_DELAYED, processToRetry);
    }

}
