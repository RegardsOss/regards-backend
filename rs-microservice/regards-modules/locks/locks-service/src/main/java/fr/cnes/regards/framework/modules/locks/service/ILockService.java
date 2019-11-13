package fr.cnes.regards.framework.modules.locks.service;

/**
 * Because i'm awesome
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Marc SORDI
 */
public interface ILockService {

    /**
    * Try to obtain a lock with specified name for specified class owner. Skip immediately if cannot obtain the lock!
    * @param name name of the lock (unique per owner)
    * @param owner owner object of the lock
    * @param expiresIn seconds before lock expiration (at least 1 second)
    * @return <code>true</code> if lock has been obtained
    */
    boolean obtainLockOrSkip(String name, Object owner, long expiresIn);

    /**
     * Synchronous method that will try to acquire a lock. In case a lock is already set, it will loop until it can get one
     * @param name name of the lock (unique per owner)
     * @param owner owner object of the lock
     * @param expiresIn seconds before lock expiration (at least 1 second)
     * @param retry millisecond between 2 attempts to obtain the lock
     * @return <code>true</code> if lock has been obtained
     */
    boolean waitForlock(String name, Object owner, long expiresIn, long retry);

    /**
     * Try to release the lock with specified name for specified class owner
     * @param name name of the lock (unique per owner)
     * @param owner owner object of the lock
     */
    void releaseLock(String name, Object owner);

    /**
    * Try to obtain a lock with specified name for specified class owner. Skip immediately if cannot obtain the lock!
    * @param name name of the lock (unique per owner)
    * @param owner owner object of the lock
    * @param expiresIn seconds before lock expiration (at least 1 second)
    * @return <code>true</code> if lock has been obtained
    */
    boolean obtainLockOrSkipTransactional(String name, Object owner, long expiresIn);
}
