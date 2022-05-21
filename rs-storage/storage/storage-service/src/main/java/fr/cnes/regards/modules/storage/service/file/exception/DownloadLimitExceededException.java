package fr.cnes.regards.modules.storage.service.file.exception;

public abstract class DownloadLimitExceededException extends RuntimeException {

    private DownloadLimitExceededException(String message) {
        super(message);
    }

    public static class DownloadQuotaExceededException extends DownloadLimitExceededException {

        private DownloadQuotaExceededException(String message) {
            super(message);
        }
    }

    public static class DownloadRateExceededException extends DownloadLimitExceededException {

        private DownloadRateExceededException(String message) {
            super(message);
        }
    }

    public static DownloadLimitExceededException buildDownloadQuotaExceededException(String userEmail,
                                                                                     long maxQuota,
                                                                                     long quota) {
        return new DownloadQuotaExceededException(String.format("Download quota of %d exceeded (by %d) for user %s.",
                                                                maxQuota,
                                                                quota - maxQuota,
                                                                userEmail));
    }

    public static DownloadRateExceededException buildDownloadRateExceededException(String userEmail,
                                                                                   long rateLimit,
                                                                                   long rate) {
        return new DownloadRateExceededException(String.format("Download rate of %d exceeded (by %d) for user %s.",
                                                               rateLimit,
                                                               rate - rateLimit,
                                                               userEmail));
    }
}
