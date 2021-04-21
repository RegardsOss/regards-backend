package fr.cnes.regards.modules.feature.service;

import java.util.Set;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IRequestRetryService {

    void retryRequest(Set<AbstractFeatureRequest> requestsToRetry);
}
