package fr.cnes.regards.modules.storage.dao.entity.mapping;

import fr.cnes.regards.modules.storage.dao.entity.download.DefaultDownloadQuotaLimitsEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.UserDownloadQuotaEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.UserDownloadRateEntity;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserDownloadQuota;
import fr.cnes.regards.modules.storage.domain.database.UserDownloadRate;

public interface DomainEntityMapper {

    UserDownloadQuotaEntity toEntity(UserDownloadQuota quota);

    UserDownloadQuota toDomain(UserDownloadQuotaEntity quota);

    DefaultDownloadQuotaLimitsEntity toEntity(DefaultDownloadQuotaLimits limits);

    DefaultDownloadQuotaLimits toDomain(DefaultDownloadQuotaLimitsEntity limits);

    DownloadQuotaLimitsEntity toEntity(DownloadQuotaLimits limits);

    DownloadQuotaLimits toDomain(DownloadQuotaLimitsEntity limits);

    UserDownloadRateEntity toEntity(UserDownloadRate rate);

    UserDownloadRate toDomain(UserDownloadRateEntity rate);

}

