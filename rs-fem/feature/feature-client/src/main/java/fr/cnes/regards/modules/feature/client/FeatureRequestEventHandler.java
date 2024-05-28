package fr.cnes.regards.modules.feature.client;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Handler {@link FeatureRequestEvent} events
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class FeatureRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FeatureRequestEvent> {

    private final ISubscriber subscriber;

    private final IFeatureRequestEventListener featureRequestEventListener;

    private final FeatureRequestEventHandlerService featureRequestEventHandlerService;

    public FeatureRequestEventHandler(ISubscriber subscriber,
                                      @Nullable IFeatureRequestEventListener featureRequestEventListener,
                                      FeatureRequestEventHandlerService featureRequestEventHandlerService) {
        this.subscriber = subscriber;
        this.featureRequestEventListener = featureRequestEventListener;
        this.featureRequestEventHandlerService = featureRequestEventHandlerService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if (featureRequestEventListener != null) {
            subscriber.subscribeTo(FeatureRequestEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FeatureRequestEvent bus messages !!");
        }
    }

    @Override
    public Errors validate(FeatureRequestEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<FeatureRequestEvent> messages) {
        LOGGER.debug("[STORAGE RESPONSES HANDLER] Handling {} FileReferenceUpdateEventHandler...", messages.size());
        long start = System.currentTimeMillis();
        featureRequestEventHandlerService.handle(messages, featureRequestEventListener);
        LOGGER.debug("[STORAGE RESPONSES HANDLER] {} FileReferenceUpdateEventHandler handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }

}
