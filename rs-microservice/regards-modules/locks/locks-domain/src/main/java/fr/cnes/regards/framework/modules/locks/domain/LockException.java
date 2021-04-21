package fr.cnes.regards.framework.modules.locks.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class LockException extends Exception {

    private static final long serialVersionUID = 1L;

    public LockException(InterruptedException e) {
        super(e);
    }
}
