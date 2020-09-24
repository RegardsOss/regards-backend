package fr.cnes.regards.modules.storage.dao.entity.mapping;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.dao.entity.download.DefaultDownloadQuotaLimitsEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.UserDownloadQuotaEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.UserDownloadRateEntity;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.DownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserDownloadQuota;
import fr.cnes.regards.modules.storage.domain.database.UserDownloadRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DomainEntityMapperImpl implements DomainEntityMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEntityMapperImpl.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public UserDownloadQuotaEntity toEntity(UserDownloadQuota quota) {
        return new UserDownloadQuotaEntity(
            quota.getId(),
            quota.getInstance(),
            quota.getEmail(),
            quota.getCounter()
        );
    }

    @Override
    public UserDownloadQuota toDomain(UserDownloadQuotaEntity quota) {
        return new UserDownloadQuota(
            quota.getId(),
            quota.getInstance(),
            runtimeTenantResolver.getTenant(),
            quota.getEmail(),
            quota.getCounter()
        );
    }

    @Override
    public DownloadQuotaLimitsEntity toEntity(DownloadQuotaLimits limits) {
        return new DownloadQuotaLimitsEntity(
            limits.getId(),
            limits.getEmail(),
            limits.getMaxQuota(),
            limits.getRateLimit()
        );
    }

    @Override
    public DownloadQuotaLimits toDomain(DownloadQuotaLimitsEntity limits) {
        return new DownloadQuotaLimits(
            limits.getId(),
            runtimeTenantResolver.getTenant(),
            limits.getEmail(),
            limits.getMaxQuota(),
            limits.getRateLimit()
        );
    }

    @Override
    public DefaultDownloadQuotaLimitsEntity toEntity(DefaultDownloadQuotaLimits limits) {
        return new DefaultDownloadQuotaLimitsEntity(
            limits.getMaxQuota(),
            limits.getRateLimit()
        );
    }

    @Override
    public DefaultDownloadQuotaLimits toDomain(DefaultDownloadQuotaLimitsEntity limits) {
        return new DefaultDownloadQuotaLimits(
            limits.getMaxQuota(),
            limits.getRateLimit()
        );
    }

    @Override
    public UserDownloadRateEntity toEntity(UserDownloadRate rate) {
        return new UserDownloadRateEntity(
            rate.getId(),
            rate.getInstance(),
            rate.getEmail(),
            rate.getGauge(),
            rate.getExpiry()
        );
    }

    @Override
    public UserDownloadRate toDomain(UserDownloadRateEntity rate) {
        return new UserDownloadRate(
            rate.getId(),
            rate.getInstance(),
            runtimeTenantResolver.getTenant(),
            rate.getEmail(),
            rate.getGauge(),
            rate.getExpiry()
        );
    }
}
