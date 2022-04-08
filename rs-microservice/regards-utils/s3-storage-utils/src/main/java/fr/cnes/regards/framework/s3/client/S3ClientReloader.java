package fr.cnes.regards.framework.s3.client;

import fr.cnes.regards.framework.s3.domain.StorageConfig;
import fr.cnes.regards.framework.s3.exception.S3ClientException;
import io.vavr.CheckedFunction1;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * This class allows to manage a viable S3 client instance for a given
 * S3 server configuration.
 *
 * <p>In certain cases, an S3 SDK client instance can break, especially after
 * SSL handshake errors. If that happens, the AmazonS3 instance repeatedly
 * throws sdk exceptions and ceases to function completely.</p>
 *
 * <p>In order to hide this problem from the S3 storage plugins,
 * this class monitors the errors and reboots a fresh S3 client instance if too many
 * successive errors are detected.</p>
 */
public class S3ClientReloader<S extends SdkClient> implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientReloader.class);

    private final int maxConsecutiveErrors;

    private final StorageConfig config;

    private final CheckedFunction1<StorageConfig, S> newClient;

    /* All uses of s3 must be in a synchronized method */
    private S s3;

    /* All uses of consecutiveSdkErrors must be in a synchronized method */
    private short consecutiveSdkErrors;

    public S3ClientReloader(int maxConsecutiveErrors, StorageConfig config,
            CheckedFunction1<StorageConfig, S> newClient) {
        this.maxConsecutiveErrors = maxConsecutiveErrors;
        this.config = config;
        this.newClient = newClient;
    }

    public <T> T withClient(CheckedFunction1<S, T> action) {
        return tryWithClient(action).get();
    }

    public <T> Try<T> tryWithClient(CheckedFunction1<S, T> action) {
        return getS3().flatMapTry(s3 -> { // NOSONAR shadowing name is intentional
            try {
                T result = action.apply(s3);
                resetErrors();
                return Try.success(result);
            } catch (S3ClientException e) {
                return Try.failure(e);
            } catch (SdkException e) {
                recordError(s3);
                return logAndWrapInStorageClientException(s3, e);
            } catch (Throwable t) {
                return logAndWrapInStorageClientException(s3, t);
            }
        });
    }

    public <T> Try<T> logAndWrapInStorageClientException(S s3, Throwable t) {
        LOGGER.error("S3Config {} - {} instance {} - {}", config.getKey(), s3.getClass().getSimpleName(), s3.hashCode(),
                     t.getMessage(), t);
        return Try.failure(new S3ClientException(t));
    }

    private synchronized void resetErrors() {
        consecutiveSdkErrors = 0;
    }

    private synchronized Try<S> getS3() {
        if (s3 != null) {
            return Try.success(s3);
        }
        return Try.success(config).mapTry(newClient)
                .peek(c -> LOGGER.debug("S3Config {} - {} instance {} created", config.getKey(),
                                        c.getClass().getSimpleName(), c.hashCode())).peek(c -> s3 = c);
    }

    private synchronized void recordError(S client) {
        if (s3 == client) {
            consecutiveSdkErrors += 1;
            if (consecutiveSdkErrors > maxConsecutiveErrors) {
                LOGGER.debug("S3Config {} - {} instance {} discarded because {} successive errors occurred",
                             config.getKey(), s3.getClass().getSimpleName(), s3.hashCode(), consecutiveSdkErrors);
                consecutiveSdkErrors = 0;

                s3.close();
                s3 = null;
            }
        }
    }

    @Override
    public void close() {
        Option.of(s3).peek(SdkClient::close);
    }
}
