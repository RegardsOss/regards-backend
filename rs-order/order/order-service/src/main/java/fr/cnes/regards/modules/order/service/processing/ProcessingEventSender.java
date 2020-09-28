package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.processing.domain.events.DownloadedOutputFilesEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessingEventSender implements IProcessingEventSender {

    private final IPublisher publisher;

    @Autowired
    public ProcessingEventSender(IPublisher publisher) {
        this.publisher = publisher;
    }

    @Override public Try<PExecutionRequestEvent> sendProcessingRequest(PExecutionRequestEvent event) {
        return Try.of(() -> {
            publisher.publish(event);
            return event;
        });
    }

    @Override public Try<DownloadedOutputFilesEvent> sendDownloadedFilesNotification(DownloadedOutputFilesEvent event) {
        return Try.of(() -> {
            publisher.publish(event);
            return event;
        });
    }
}
