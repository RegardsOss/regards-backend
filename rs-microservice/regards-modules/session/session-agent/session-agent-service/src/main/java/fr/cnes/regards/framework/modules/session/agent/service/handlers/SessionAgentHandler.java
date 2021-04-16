package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Iliana Ghazali
 **/
public class SessionAgentHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StepPropertyUpdateRequestEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StepPropertyUpdateRequestEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, StepPropertyUpdateRequestEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<StepPropertyUpdateRequestEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.info("[STEP EVENT HANDLER] Handling {} StepEvents...", messages.size());
            long start = System.currentTimeMillis();
            handle(messages);
            LOGGER.info("[STEP EVENT HANDLER] {} StepEvents handled in {} ms", messages.size(),
                         System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link StepPropertyUpdateRequestEvent}s
     */
    private void handle(List<StepPropertyUpdateRequestEvent> events) {
        List<StepPropertyUpdateRequest> stepPropertiesToSave = new ArrayList<>();
        for (StepPropertyUpdateRequestEvent e : events) {
            stepPropertiesToSave
                    .add(new StepPropertyUpdateRequest(e.getStepId(), e.getSource(), e.getSession(), e.getDate(),
                                                       e.getStepType(), e.getState(), e.getProperty(), e.getValue(),
                                                       e.getEventTypeEnum(), e.isInput_related(),
                                                       e.isOutput_related()));

        }
        this.stepPropertyRepo.saveAll(stepPropertiesToSave);
    }
}