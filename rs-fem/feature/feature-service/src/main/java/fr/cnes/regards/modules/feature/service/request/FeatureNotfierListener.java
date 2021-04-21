package fr.cnes.regards.modules.feature.service.request;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class FeatureNotfierListener implements INotifierRequestListener {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureNotfierListener.class);

    public static final String RECEIVED_FROM_NOTIFIER_FORMAT = "Received {} {} indicating {} to handle from rs-notifier";

    public static final String HANDLED_FROM_NOTIFIER_FORMAT = "Handled {} {} {}";

    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;

    @Override
    public void onRequestDenied(List<NotifierEvent> denied) {
        handleNotificationIssue(denied);
    }

    private void handleNotificationIssue(List<NotifierEvent> denied) {
        LOG.debug(RECEIVED_FROM_NOTIFIER_FORMAT,
                  denied.size(),
                  NotifierEvent.class.getSimpleName(),
                  NotificationState.ERROR);
        List<String> requestIds = denied.stream().map(NotifierEvent::getRequestId).collect(Collectors.toList());
        Set<AbstractFeatureRequest> errorRequest = abstractFeatureRequestRepo.findAllByRequestIdIn(requestIds);
        if (!errorRequest.isEmpty()) {
            featureNotificationService.handleNotificationError(errorRequest);
            LOG.debug(HANDLED_FROM_NOTIFIER_FORMAT,
                      denied.size(),
                      NotificationState.ERROR,
                      NotifierEvent.class.getSimpleName());
        }
    }

    @Override
    public void onRequestGranted(List<NotifierEvent> granted) {
        //FIXME: i think nothing to do but lets see that later
    }

    @Override
    public void onRequestSuccess(List<NotifierEvent> success) {
        LOG.debug(RECEIVED_FROM_NOTIFIER_FORMAT,
                  success.size(),
                  NotifierEvent.class.getSimpleName(),
                  NotificationState.SUCCESS);
        List<String> requestIds = success.stream().map(NotifierEvent::getRequestId).collect(Collectors.toList());
        Set<AbstractFeatureRequest> successRequest = abstractFeatureRequestRepo.findAllByRequestIdIn(requestIds);
        if(!successRequest.isEmpty()) {
            featureNotificationService.handleNotificationSuccess(successRequest);
            LOG.debug(HANDLED_FROM_NOTIFIER_FORMAT, success.size(), NotificationState.SUCCESS, NotifierEvent.class.getSimpleName());
        }
    }

    @Override
    public void onRequestError(List<NotifierEvent> error) {
        handleNotificationIssue(error);
    }
}
