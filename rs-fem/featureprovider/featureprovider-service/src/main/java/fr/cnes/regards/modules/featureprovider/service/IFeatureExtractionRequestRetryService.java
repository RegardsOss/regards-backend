package fr.cnes.regards.modules.featureprovider.service;

import java.util.Set;

import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IFeatureExtractionRequestRetryService {

    void retryRequest(Set<FeatureExtractionRequest> requestsToRetry);
}
