package fr.cnes.regards.framework.modules.locks.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.locks.dao.ILockRepository;
import fr.cnes.regards.framework.modules.locks.domain.Lock;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class LockService implements ILockService {

    private static final Logger LOG = LoggerFactory.getLogger(LockService.class);

    @Autowired
    private ILockService self;

    @Autowired
    private ILockRepository lockRepository;

    /**
     * !!!!WARNING!!!! this method needs to return us multiple information: <br/>
     * <ul>
     *     <li>Did we apply the lock?</li>
     *     <li>If we did, what is the lock id to be able to release it later.</li>
     * </ul>
     * To do so, we are returning whether the lock was applied and modifying <b>lock</b> parameter if the lock has been applied. <br/>
     * This method uses {@link Isolation#SERIALIZABLE} to ensure that no one else can try to but a lock at teh same time we are.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public boolean obtainLockOrSkipTransactional(String name, Object owner, long expiresIn) {
        Assert.hasText(name, "Lock name is required");
        Assert.notNull(owner, "Class owner is required");
        Assert.notNull(expiresIn, "Expiration time is required");
        Assert.isTrue(expiresIn >= 1, "Expiration time must be at least of 1 second");

        Optional<Lock> currentLock = lockRepository.findByLockingClassNameAndLockName(owner.getClass().getName(), name);

        if (currentLock.isPresent()) {
            // Prevent keeping expirated lock
            if ((currentLock.get().getExpirationDate() != null)
                    && currentLock.get().getExpirationDate().isBefore(OffsetDateTime.now())) {
                // If lock has expired, replace with the new one
                Lock newLock = currentLock.get();
                newLock.expiresIn(expiresIn);
                lockRepository.save(newLock);
                return true;
            } else {
                // Skip
                return false;
            }
        }

        Lock newLock = new Lock(name, owner.getClass());
        newLock.expiresIn(expiresIn);
        lockRepository.save(newLock);
        return true;
    }

    @Override
    public boolean obtainLockOrSkip(String name, Object owner, long expiresIn) {
        try {
            return self.obtainLockOrSkipTransactional(name, owner, expiresIn);
        } catch (LockAcquisitionException | CannotAcquireLockException | JpaSystemException e) {
            LOG.warn(String.format("Error getting database lock %s. Cause: %s.", name, e.getMessage()));
            return false;
        }
    }

    @Override
    public boolean waitForlock(String name, Object owner, long expiresIn, long retry) {
        boolean lockByThisCall = false;
        while (!lockByThisCall) {
            lockByThisCall = obtainLockOrSkip(name, owner, expiresIn);
            if (!lockByThisCall) {
                try {
                    Thread.sleep(retry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.error("Lock could not be acquired", e);
                    return false;
                }
            }
        }
        return lockByThisCall;
    }

    @Override
    public void releaseLock(String name, Object owner) {
        Optional<Lock> currentLock = lockRepository.findByLockingClassNameAndLockName(owner.getClass().getName(), name);
        if (currentLock.isPresent()) {
            lockRepository.delete(currentLock.get());
        }
    }
}