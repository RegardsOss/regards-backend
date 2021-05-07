package fr.cnes.regards.framework.modules.session.commons.service.jobs;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Listen to JobEvent Completion to update {@link fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
public class SnapshotJobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent>  {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SnapshotJobEventService snapshotJobEventService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, JobEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<JobEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.trace("[AGENT SNAPSHOT JOB EVENT HANDLER] Handling {} JobEvents...", messages.size());
            long start = System.currentTimeMillis();
            // sort job
            snapshotJobEventService.updateSnapshotProcess(messages);
            LOGGER.trace("[AGENT SNAPSHOT JOB EVENT HANDLER] {} JobEvents handled in {} ms", messages.size(),
                        System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }


}