package fr.cnes.regards.modules.processing.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.events.DownloadedOutputFilesEvent;
import fr.cnes.regards.modules.processing.service.IOutputFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DownloadedOutputFileEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<DownloadedOutputFilesEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadedOutputFileEventHandler.class);


    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final ISubscriber subscriber;
    private final IOutputFileService outFileService;

    public DownloadedOutputFileEventHandler(IOutputFileService outFileService,
            IRuntimeTenantResolver runtimeTenantResolver, ISubscriber subscriber) {
        this.outFileService = outFileService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
    }

    @Override public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DownloadedOutputFilesEvent.class, this);
    }

    @Override public void handle(String tenant, DownloadedOutputFilesEvent message) {
        runtimeTenantResolver.forceTenant(tenant); // Needed in order to publish events

        LOGGER.info("Downloaded outputfile event received: {}", message);

        outFileService.markDownloaded(message.getOutputFileIds())
                .subscribe(
                        exec -> LOGGER.info("Output files marked as downloaded: {}", message),
                        err -> LOGGER.error(err.getMessage(), err)
                );
    }
}
