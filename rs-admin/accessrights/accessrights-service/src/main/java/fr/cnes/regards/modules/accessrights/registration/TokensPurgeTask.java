/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;

/**
 * Cron task purging the expired token repositories.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@Transactional
public class TokensPurgeTask {

    /**
     * The token repository
     */
    @Autowired
    private IVerificationTokenRepository tokenRepository;

    // @Autowired
    // PasswordResetTokenRepository passwordTokenRepository;

    @Scheduled(cron = "${purge.cron.expression}")
    public void purgeExpired() {

        final Date now = Date.from(Instant.now());

        // passwordTokenRepository.deleteAllExpiredSince(now);
        tokenRepository.deleteAllExpiredSince(now);
    }
}
