package fr.cnes.regards.modules.storage.service.file.download;

import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserQuotaAggregate;
import fr.cnes.regards.modules.storage.domain.database.UserRateAggregate;
import io.vavr.Tuple2;

import java.util.concurrent.Future;

public interface IQuotaManager {

    /**
     * @return best available view of the system, quota and rate wise, at this time
     */
    Tuple2<UserQuotaAggregate, UserRateAggregate> get(DownloadQuotaLimits quota);

    void increment(DownloadQuotaLimits quota);

    void decrement(DownloadQuotaLimits quota);
}
