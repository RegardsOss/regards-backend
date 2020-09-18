package fr.cnes.regards.modules.storage.service.file.download;

import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import io.vavr.control.Try;

import java.util.function.Function;

public interface IQuotaService<T> {
    UserCurrentQuotas getCurrentQuotas(String userEmail);

    /**
     * Executes <code>operation</code> if and only if the current gauge for the
     * target user (specified by its <code>userEmail</code>) is not superior
     * to its configured quota.
     *
     * <p>This function does not keep the gauge in sync by itself and delegates it this
     * way in order to let caller define by itself when the operation start and stops,
     * allowing complex, async or any kind of operation without interfering.
     * </p>
     *
     * <p>The caller is supplied with a {@link WithQuotaOperationHandler} to
     * keep the gauge in sync by calling its <code>start()</code>
     * and <code>stop()</code> methods when the "quota restricted" operation
     * begins and ends, respectively.
     * </p>
     *
     * <p>This, however, comes at the cost of potential gauge sync incoherence if
     * the caller forgets to call <code>start()</code> or
     * <code>stop()</code>, or if the caller does not
     * wrap its operation properly in error handling code.
     * </p>
     *
     * <p>Using a {@link Try} is a hint that the caller has to handle and wrap errors
     * properly in order to call <code>start()</code>
     * and <code>stop()</code> when appropriate.
     * </p>
     *
     * @param userEmail email of the user whose quota should be checked and kept in sync
     * @param operation caller action which will be executed
     * @return the result of the supplied <code>operation</code> execution
     */
    Try<T> withQuota(String userEmail, Function<WithQuotaOperationHandler, Try<T>> operation);

    interface WithQuotaOperationHandler {
        void start();
        void stop();
    }
}
