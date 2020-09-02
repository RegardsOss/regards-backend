package fr.cnes.regards.modules.feature.service.request;

import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class FeatureNotfierListener implements INotifierRequestListener {

    //TODO autowired abstract request repo
    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Override
    public void onRequestDenied(List<NotifierEvent> denied) {

    }

    @Override
    public void onRequestGranted(List<NotifierEvent> granted) {
        //FIXME: i think nothing to do but lets see that later
    }

    @Override
    public void onRequestSuccess(List<NotifierEvent> success) {
        List<String> requestIds = success.stream().map(event->event.getRequestId()).collect(Collectors.toList());
        Set<AbstractFeatureRequest> successRequest = abstractFeatureRequestRepo.findAllByRequestId(requestIds);
        featureNotificationService.handleNotificationSuccess(successRequest);
    }

    @Override
    public void onRequestError(List<NotifierEvent> error) {
        featureNotificationService.handleNotificationError();
    }
}
