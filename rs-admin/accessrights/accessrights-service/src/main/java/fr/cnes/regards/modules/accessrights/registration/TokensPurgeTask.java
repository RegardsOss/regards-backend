/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.modules.accessrights.dao.instance.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;

/**
 * Cron task purging the expired token repositories.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Service
@Transactional
public class TokensPurgeTask {

    /**
     * The verification token repository
     */
    @Autowired
    private IVerificationTokenRepository tokenRepository;

    /**
     * The password reset token repository
     */
    @Autowired
    private IPasswordResetTokenRepository passwordTokenRepository;

    @Scheduled(cron = "${purge.cron.expression}")
    public void purgeExpired() {

        final LocalDateTime now = LocalDateTime.now();

        passwordTokenRepository.deleteAllExpiredSince(now);
        tokenRepository.deleteAllExpiredSince(now);
    }
}
