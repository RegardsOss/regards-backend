package fr.cnes.regards.modules.feature.service.request;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class FeatureNotfierListener implements INotifierRequestListener {

    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Override
    public void onRequestGranted() {
        //FIXME: i think nothing to do but lets see that later
    }

    @Override
    public void onRequestSuccess() {
        featureNotificationService.handleNotificationSuccess();
    }

    @Override
    public void onRequestError() {
        featureNotificationService.handleNotificationError();
    }
}
