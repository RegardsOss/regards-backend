/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional;

/**
 *
 * Class TestDaoException
 *
 * DAO Exception for tests.
 *
 * @author CS
 * @since TODO
 */
public class DaoTestException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    public DaoTestException() {
        super();
    }

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            message
     * @param pCause
     *            cause
     * @since 1.0-SNAPSHOT
     */
    public DaoTestException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            message
     * @since 1.0-SNAPSHOT
     */
    public DaoTestException(String pMessage) {
        super(pMessage);
    }

}
