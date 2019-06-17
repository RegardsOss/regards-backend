package fr.cnes.regards.framework.modules.locks.service;

import fr.cnes.regards.framework.modules.locks.domain.Lock;
import fr.cnes.regards.framework.modules.locks.domain.LockException;

/**
 * Because i'm awesome
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ILockService {

    /**
     * Synchronous method that will try to acquire a lock. In case a lock is already set, it will loop until it can get one
     * @param lock lock to be acquired
     * @param seconds seconds until lock expiration. Negative value or 0 means there is no expiration
     * @return acquired lock
     * @throws LockException thrown when we were unable to wait for the lock to be acquired
     */
    Lock lock(Lock lock, long seconds) throws LockException;

    boolean tryLock(Lock lock, long seconds);

    /**
     * Release given lock
     */
    void release(Lock lock);

}
