package fr.cnes.regards.modules.feature.client;

import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IFeatureRequestEventListener {

    void onRequestDenied(List<FeatureRequestEvent> denied);

    void onRequestSuccess(List<FeatureRequestEvent> success);

    void onRequestError(List<FeatureRequestEvent> error);

    void onRequestGranted(List<FeatureRequestEvent> granted);
}
