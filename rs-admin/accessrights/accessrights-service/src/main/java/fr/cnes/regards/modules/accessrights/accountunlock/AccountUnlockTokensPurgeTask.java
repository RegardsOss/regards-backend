/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.accountunlock;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.modules.accessrights.dao.accountunlock.IAccountUnlockTokenRepository;

/**
 * Cron task purging the expired token repository.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Service
@InstanceTransactional
public class AccountUnlockTokensPurgeTask {

    /**
     * The password reset token repository
     */
    @Autowired
    private IAccountUnlockTokenRepository repository;

    @Scheduled(cron = "${purge.cron.expression}")
    public void purgeExpired() {
        final LocalDateTime now = LocalDateTime.now();
        repository.deleteAllExpiredSince(now);
    }
}
