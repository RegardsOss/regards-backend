package fr.cnes.regards.framework.modules.locks.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
    public boolean obtainLockOrSkip(String name, Class<?> owner, long expiresIn) {
        Assert.hasText(name, "Lock name is required");
        Assert.notNull(owner, "Class owner is required");
        Assert.notNull(expiresIn, "Expiration time is required");
        Assert.isTrue(expiresIn >= 1, "Expiration time must be at least of 1 second");

        Optional<Lock> currentLock = lockRepository.findByLockingClassNameAndLockName(owner.getName(), name);

        if (currentLock.isPresent()) {
            // Prevent keeping expirated lock
            if (currentLock.get().getExpirationDate() != null
                    && currentLock.get().getExpirationDate().isBefore(OffsetDateTime.now())) {
                // If lock has expired, lets remove it
                lockRepository.delete(currentLock.get());
            } else {
                // Skip
                return false;
            }
        }

        Lock newLock = new Lock(name, owner);
        newLock.expiresIn(expiresIn);
        lockRepository.save(newLock);
        return true;
    }

    @Override
    public boolean waitForlock(String name, Class<?> owner, long expiresIn, long retry) {
        boolean lockByThisCall = false;
        while (!lockByThisCall) {
            lockByThisCall = self.obtainLockOrSkip(name, owner, expiresIn);
            try {
                Thread.sleep(retry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return lockByThisCall;
    }

    @Override
    public void releaseLock(String name, Class<?> owner) {
        Optional<Lock> currentLock = lockRepository.findByLockingClassNameAndLockName(owner.getName(), name);
        if (currentLock.isPresent()) {
            lockRepository.delete(currentLock.get());
        }
    }
}