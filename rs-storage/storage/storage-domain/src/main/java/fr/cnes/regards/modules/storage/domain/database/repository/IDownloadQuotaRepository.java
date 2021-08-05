package fr.cnes.regards.modules.storage.domain.database.repository;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.storage.domain.database.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface IDownloadQuotaRepository {

    DownloadQuotaLimits save(DownloadQuotaLimits quota);

    Optional<DownloadQuotaLimits> findByEmail(String email);

    Map<String, Long> fetchMaxQuotaByUser();

    void deleteByEmail(String email);

    UserQuotaAggregate fetchDownloadQuotaSum(String email);

    UserRateAggregate fetchDownloadRatesSum(String email);

    UserDownloadQuota upsertOrCombineDownloadQuota(String instanceId, String email, Long diff);

    UserDownloadRate upsertOrCombineDownloadRate(String instanceId, String email, Long diff, LocalDateTime expiry);

    void deleteExpiredRates();

    @VisibleForTesting
    void deleteAll();
}
