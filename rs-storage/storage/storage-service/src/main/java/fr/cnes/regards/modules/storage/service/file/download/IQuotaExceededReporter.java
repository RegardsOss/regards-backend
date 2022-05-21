package fr.cnes.regards.modules.storage.service.file.download;

import fr.cnes.regards.modules.storage.service.file.exception.DownloadLimitExceededException;

public interface IQuotaExceededReporter<T> {

    default void report(DownloadLimitExceededException e, T file, String email, String tenant) {
        if (e instanceof DownloadLimitExceededException.DownloadQuotaExceededException) {
            report((DownloadLimitExceededException.DownloadQuotaExceededException) e, file, email, tenant);
        } else if (e instanceof DownloadLimitExceededException.DownloadRateExceededException) {
            report((DownloadLimitExceededException.DownloadRateExceededException) e, file, email, tenant);
        }
    }

    void report(DownloadLimitExceededException.DownloadQuotaExceededException e, T file, String email, String tenant);

    void report(DownloadLimitExceededException.DownloadRateExceededException e, T file, String email, String tenant);
}
