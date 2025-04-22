package fr.cnes.regards.modules.fileaccess.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.fileaccess.dto.quota.UserCurrentQuotasDto;
import io.vavr.control.Try;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * FIXME Lot 2
 * Uses 0 quota for now
 */
@Service
@MultitenantTransactional
public class DownloadQuotaService<T> {

    public DownloadQuotaService() {
        // empty constructor
    }

    public Try<DownloadQuotaLimitsDto> getDownloadQuotaLimits(String userEmail) {
        return Try.of(() -> new DownloadQuotaLimitsDto(userEmail, 0L, 0L));
    }

    public Try<List<DownloadQuotaLimitsDto>> getDownloadQuotaLimits(String[] userEmails) {
        return Try.of(() -> Arrays.stream(userEmails)
                                  .map(userEmail -> new DownloadQuotaLimitsDto(userEmail, 0L, 0L))
                                  .toList());
    }

    public UserCurrentQuotasDto getCurrentQuotas(String userEmail) {

        return new UserCurrentQuotasDto(userEmail, 0L, 0L, 0L, 0L);
    }

    public Try<List<UserCurrentQuotasDto>> getCurrentQuotas(String[] userEmails) {
        return Try.of(() -> Arrays.stream(userEmails).map(this::getCurrentQuotas).toList());
    }

    public Try<DownloadQuotaLimitsDto> upsertDownloadQuotaLimits(DownloadQuotaLimitsDto newLimits) {
        return Try.of(() -> newLimits);
    }

    public Long getMaxQuota() {
        return 0L;
    }
}
