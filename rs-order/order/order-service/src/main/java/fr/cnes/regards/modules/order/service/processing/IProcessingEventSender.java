package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.modules.processing.domain.events.DownloadedOutputFilesEvent;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import io.vavr.control.Try;

public interface IProcessingEventSender {

    Try<PExecutionRequestEvent> sendProcessingRequest(PExecutionRequestEvent event);

    Try<DownloadedOutputFilesEvent> sendDownloadedFilesNotification(DownloadedOutputFilesEvent event);

}
