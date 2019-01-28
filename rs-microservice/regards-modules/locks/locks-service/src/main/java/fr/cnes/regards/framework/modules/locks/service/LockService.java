package fr.cnes.regards.framework.modules.locks.service;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.locks.dao.ILockRepository;
import fr.cnes.regards.framework.modules.locks.domain.Lock;
import fr.cnes.regards.framework.modules.locks.domain.LockException;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class LockService implements ILockService {

    @Autowired
    private ILockRepository lockRepository;

    @Override
    public Lock lock(Lock lock, long seconds) throws LockException {
        boolean lockByThisCall = false;
        while (!lockByThisCall) {
            lockByThisCall = tryLock(lock, seconds);
            // lets wait for 2 seconds
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockException(e);
            }
        }
        return lock;
    }

    /**
     * !!!!WARNING!!!! this method needs to return us multiple information: <br/>
     * <ul>
     *     <li>Did we apply the lock?</li>
     *     <li>If we did, what is the lock id to be able to release it later.</li>
     * </ul>
     * To do so, we are returning whether the lock was applied and modifying <b>lock</b> parameter if the lock has been applied. <br/>
     * This method uses {@link Isolation#SERIALIZABLE} to ensure that no one else can try to but a lock at teh same time we are.
     * @param lock lock to be acquired
     * @param seconds seconds until lock expiration. Negative value or 0 means there is no expiration
     * @return true if and only if this method call has been able to apply the lock
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public boolean tryLock(Lock lock, long seconds) {
        Lock currentLock;
        currentLock = lockRepository.findByLockingClassNameAndLockName(lock.getLockingClassName(), lock.getLockName());
        if ((currentLock != null) && (currentLock.getExpirationDate() != null)
                && currentLock.getExpirationDate().isBefore(OffsetDateTime.now())) {
            // if lock has expired, lets remove it
            lockRepository.delete(currentLock);
            currentLock = null;
        }
        if (currentLock == null) {
            // if the lock should expire, lets add expiration date
            if (seconds > 0) {
                lock.expiresIn(seconds);
            }
            // if there is no lock, lets modify parameter to get an id from database
            lock = lockRepository.save(lock);
        }
        return currentLock == null;
    }

    @Override
    public void release(Lock lock) {
        lockRepository.deleteById(lock.getId());
    }
}