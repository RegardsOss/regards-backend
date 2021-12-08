package fr.cnes.regards.modules.notifier.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
@RegardsTransactional
public class TestService  {

    @Autowired
    private INotificationRequestRepository notificationRequestRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDatabaseToSimulateProcessFailForRecipient(List<NotificationRequest> requests,
            PluginConfiguration recipientThatFailed) {
        // we need to set requests in error and add one recipient as error
        for(NotificationRequest request: requests) {
            request.setState(NotificationState.ERROR);
            request.getRecipientsScheduled().remove(recipientThatFailed);
            request.getRecipientsInError().add(recipientThatFailed);
        }
        notificationRequestRepository.saveAll(requests);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDatabaseToSimulateRetryOnlyRulesToMatch(List<NotificationRequest> requests) {
        // retry will only change the same of the request as we do not have recipients in error but rule to match
        for(NotificationRequest request: requests) {
            request.setState(NotificationState.GRANTED);
        }
        notificationRequestRepository.saveAll(requests);
    }
}
