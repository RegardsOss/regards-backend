package fr.cnes.regards.modules.storage.domain.database.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserDownloadQuota;
import fr.cnes.regards.modules.storage.domain.database.UserDownloadRate;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;

public interface IDownloadQuotaRepository {

    DownloadQuotaLimits save(DownloadQuotaLimits quota);

    Optional<DownloadQuotaLimits> findByEmail(String email);

    void deleteByEmail(String email);

    UserQuotaAggregate fetchDownloadQuotaSum(String email);

    UserRateAggregate fetchDownloadRatesSum(String email);

    UserDownloadQuota upsertOrCombineDownloadQuota(String instanceId, String email, Long diff);

    UserDownloadRate upsertOrCombineDownloadRate(String instanceId, String email, Long diff, LocalDateTime expiry);

    void deleteExpiredRates();

    @VisibleForTesting
    void deleteAll();
}
