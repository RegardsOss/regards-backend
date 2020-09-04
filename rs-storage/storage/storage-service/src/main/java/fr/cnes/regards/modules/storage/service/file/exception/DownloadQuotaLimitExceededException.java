package fr.cnes.regards.modules.storage.service.file.exception;

public class DownloadQuotaLimitExceededException extends Exception {

    private DownloadQuotaLimitExceededException(String message) {
        super(message);
    }

    public static DownloadQuotaLimitExceededException buildDownloadQuotaExceededException(String userEmail, long maxQuota, long quota) {
        return new DownloadQuotaLimitExceededException(String.format(
            "Download quota of %d exceeded (by %d) for user %s.",
            maxQuota,
            quota - maxQuota,
            userEmail
        ));
    }

    public static DownloadQuotaLimitExceededException buildDownloadRateExceededException(String userEmail, long rateLimit, long rate) {
        return new DownloadQuotaLimitExceededException(String.format(
            "Download rate of %d exceeded (by %d) for user %s.",
            rateLimit,
            rate - rateLimit,
            userEmail
        ));
    }
}
