package fr.cnes.regards.modules.feature.client;

import java.util.List;

import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IFeatureRequestEventListener {

    void onRequestDenied(List<FeatureRequestEvent> denied);

    void onRequestSuccess(List<FeatureRequestEvent> success);

    void onRequestError(List<FeatureRequestEvent> error);

    void onRequestGranted(List<FeatureRequestEvent> granted);
}
