package fr.cnes.regards.modules.feature.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class FeatureRequestEventHandler implements ApplicationListener<ApplicationReadyEvent>,
        IBatchHandler<FeatureRequestEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired(required = false)
    private IFeatureRequestEventListener featureRequestEventListener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if (featureRequestEventListener != null) {
            subscriber.subscribeTo(FeatureRequestEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FeatureRequestEvent bus messages !!");
        }
    }

    @Override
    public boolean validate(String tenant, FeatureRequestEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<FeatureRequestEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.debug("[STORAGE RESPONSES HANDLER] Handling {} FileReferenceUpdateEventHandler...", messages.size());
            long start = System.currentTimeMillis();
            handle(messages);
            LOGGER.debug("[STORAGE RESPONSES HANDLER] {} FileReferenceUpdateEventHandler handled in {} ms",
                         messages.size(),
                         System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    private void handle(List<FeatureRequestEvent> events) {
        List<FeatureRequestEvent> denied = new ArrayList<>();
        List<FeatureRequestEvent> granted = new ArrayList<>();
        List<FeatureRequestEvent> success = new ArrayList<>();
        List<FeatureRequestEvent> error = new ArrayList<>();
        // dispatch event according to which state caused them to be sent
        for (FeatureRequestEvent event : events) {
            switch (event.getState()) {
                case DENIED:
                    denied.add(event);
                    break;
                case GRANTED:
                    granted.add(event);
                    break;
                case SUCCESS:
                    success.add(event);
                    break;
                case ERROR:
                    error.add(event);
                    break;
                default:
                    break;
            }
        }
        // now manage message in right order
        if(!denied.isEmpty()) {
            manageDenied(denied);
        }
        if (!granted.isEmpty()) {
            manageGranted(granted);
        }
        if (!error.isEmpty()) {
            manageError(error);
        }
        if (!success.isEmpty()) {
            manageSuccess(success);
        }
    }

    private void manageDenied(List<FeatureRequestEvent> denied) {
        featureRequestEventListener.onRequestDenied(denied);
    }

    private void manageSuccess(List<FeatureRequestEvent> success) {
        featureRequestEventListener.onRequestSuccess(success);
    }

    private void manageError(List<FeatureRequestEvent> error) {
        featureRequestEventListener.onRequestError(error);
    }

    private void manageGranted(List<FeatureRequestEvent> granted) {
        featureRequestEventListener.onRequestGranted(granted);
    }
}
