package fr.cnes.regards.modules.featureprovider.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.feature.client.IFeatureRequestEventListener;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class FeatureRequestEventListener implements IFeatureRequestEventListener {

    @Autowired
    private IFeatureExtractionService featureReferenceService;

    @Override
    public void onRequestDenied(List<FeatureRequestEvent> denied) {
        featureReferenceService.handleDenied(denied);
    }

    @Override
    public void onRequestSuccess(List<FeatureRequestEvent> success) {
        //nothing to do request is now feature module property and not featureprovider
    }

    @Override
    public void onRequestError(List<FeatureRequestEvent> error) {
        //nothing to do request is now feature module property and not featureprovider
    }

    @Override
    public void onRequestGranted(List<FeatureRequestEvent> granted) {
        featureReferenceService.handleGranted(granted);
    }
}
