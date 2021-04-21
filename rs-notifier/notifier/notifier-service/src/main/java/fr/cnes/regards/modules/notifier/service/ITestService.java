package fr.cnes.regards.modules.notifier.service;

import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ITestService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void updateDatabaseToSimulateProcessFailForRecipient(List<NotificationRequest> requests, PluginConfiguration recipientThatFailed);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void updateDatabaseToSimulateRetryOnlyRulesToMatch(List<NotificationRequest> requests);
}
