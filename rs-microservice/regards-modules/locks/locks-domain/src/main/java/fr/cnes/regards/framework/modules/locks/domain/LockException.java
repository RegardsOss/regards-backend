package fr.cnes.regards.framework.modules.locks.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class LockException extends Exception {

    public LockException(InterruptedException e) {
        super(e);
    }
}
